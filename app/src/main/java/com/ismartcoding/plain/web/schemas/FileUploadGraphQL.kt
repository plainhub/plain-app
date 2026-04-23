package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.GraphQLError
import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.helpers.AppFileStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun SchemaBuilder.addFileUploadSchema() {
    val uploadTmpDir = File(MainApp.instance.filesDir, "upload_tmp")

    query("uploadedChunks") {
        resolver { fileId: String ->
            val chunkDir = File(uploadTmpDir, fileId)
            if (!chunkDir.exists()) return@resolver emptyList<String>()

            chunkDir.listFiles()
                ?.filter { it.name.startsWith("chunk_") } // Exclude temp files (.tmp_chunk_*)
                ?.mapNotNull { file ->
                    val index = file.name.removePrefix("chunk_").toIntOrNull()
                    if (index != null) "${index}:${file.length()}" else null
                }
                ?.sortedBy { it.substringBefore(':').toInt() }
                ?: emptyList()
        }
    }
    mutation("deleteChunks") {
        resolver { fileId: String ->
            val chunkDir = File(uploadTmpDir, fileId)
            if (chunkDir.exists()) {
                chunkDir.deleteRecursively()
            }
            true
        }
    }
    mutation("mergeChunks") {
        resolver { fileId: String, totalChunks: Int, path: String, replace: Boolean, isAppFile: Boolean ->
            val chunkDir = File(uploadTmpDir, fileId)
            if (!chunkDir.exists()) {
                throw GraphQLError("No chunks found for $fileId")
            }

            // Pre-calculate expected merged size from chunk files
            var expectedMergedSize = 0L
            for (i in 0 until totalChunks) {
                val chunkFile = File(chunkDir, "chunk_$i")
                if (!chunkFile.exists()) {
                    throw GraphQLError("Missing chunk $i")
                }
                expectedMergedSize += chunkFile.length()
            }

            val outputFile = if (replace) {
                File(path)
            } else {
                val originalFile = File(path)
                if (originalFile.exists()) {
                    File(originalFile.newPath())
                } else {
                    originalFile
                }
            }
            outputFile.parentFile?.mkdirs()

            // Merge into a temp file first, then rename atomically.
            // This prevents the file from appearing in listings with a partial size.
            val tempMergeFile = File(outputFile.parentFile, ".merge_tmp_${fileId}_${System.currentTimeMillis()}")
            try {
                FileOutputStream(tempMergeFile).use { fos ->
                    val outputChannel = fos.channel
                    for (i in 0 until totalChunks) {
                        val chunkFile = File(chunkDir, "chunk_$i")

                        chunkFile.inputStream().channel.use { inputChannel ->
                            var position = 0L
                            val size = inputChannel.size()
                            while (position < size) {
                                val transferred = inputChannel.transferTo(position, size - position, outputChannel)
                                if (transferred < 0) throw IOException("transferTo failed at position $position")
                                if (transferred == 0L) {
                                    // transferTo can transiently return 0; yield and retry
                                    Thread.sleep(1)
                                    continue
                                }
                                position += transferred
                            }
                        }
                    }
                    // Force all data to disk before checking size
                    fos.fd.sync()
                }

                val mergedSize = tempMergeFile.length()

                // Cross-check: merged file must equal sum of all chunk sizes
                if (mergedSize != expectedMergedSize) {
                    tempMergeFile.delete()
                    throw GraphQLError("Merge integrity failed: expected $expectedMergedSize, got $mergedSize")
                }

                // Atomic rename: file appears with correct size instantly
                if (outputFile.exists() && replace) {
                    outputFile.delete()
                }
                if (!tempMergeFile.renameTo(outputFile)) {
                    // renameTo can move the file but still return false on some
                    // Android file systems. Only use copyTo when source still exists.
                    if (tempMergeFile.exists()) {
                        tempMergeFile.copyTo(outputFile, overwrite = true)
                        tempMergeFile.delete()
                    } else if (!outputFile.exists()) {
                        throw GraphQLError("Failed to save merged file: rename failed and source file is missing")
                    }
                }
            } catch (e: Exception) {
                tempMergeFile.delete()
                throw e
            }

            val mergedSize = outputFile.length()

            chunkDir.deleteRecursively()
            if (isAppFile) {
                // Import into content-addressable store; returns SHA-256 hash
                val dFile = AppFileStore.importFile(MainApp.instance, outputFile, "", deleteSrc = true)
                "${dFile.id}:$mergedSize"
            } else {
                MainApp.instance.scanFileByConnection(outputFile, null)
                // Return base filename (consistent with /upload) + merged size
                "${outputFile.name}:$mergedSize"
            }
        }
    }
}
