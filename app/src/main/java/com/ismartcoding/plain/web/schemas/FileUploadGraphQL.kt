package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.helpers.AppFileStore
import java.io.File
import java.io.FileOutputStream

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

            val cacheMergeDir = File(MainApp.instance.cacheDir, "upload_merge").apply { mkdirs() }
            val tempMergeFile = File(cacheMergeDir, ".merge_tmp_${fileId}_${System.currentTimeMillis()}")
            try {
                FileOutputStream(tempMergeFile).use { fos ->
                    for (i in 0 until totalChunks) {
                        val chunkFile = File(chunkDir, "chunk_$i")
                        chunkFile.inputStream().use { input ->
                            input.copyTo(fos)
                        }
                    }
                }

                val mergedSize = tempMergeFile.length()

                if (mergedSize != expectedMergedSize) {
                    tempMergeFile.delete()
                    throw GraphQLError("Merge integrity failed: expected $expectedMergedSize, got $mergedSize")
                }

                if (outputFile.exists() && replace) {
                    outputFile.delete()
                }
                tempMergeFile.copyTo(outputFile, overwrite = true)
                FileOutputStream(outputFile, true).use { it.fd.sync() }
                tempMergeFile.delete()
            } catch (e: Exception) {
                tempMergeFile.delete()
                throw e
            }

            val mergedSize = outputFile.length()

            chunkDir.deleteRecursively()
            if (isAppFile) {
                // Import into content-addressable store; returns "{hash}.{ext}" as fid suffix
                val dFile = AppFileStore.importFile(outputFile, "", deleteSrc = true)
                val fidSuffix = java.io.File(dFile.realPath).name  // "{hash}.{ext}"
                "${fidSuffix}:$mergedSize"
            } else {
                MainApp.instance.scanFileByConnection(outputFile, null)
                // Return base filename (consistent with /upload) + merged size
                "${outputFile.name}:$mergedSize"
            }
        }
    }
}
