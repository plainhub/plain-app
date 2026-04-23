package com.ismartcoding.plain.web.routes

import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.UploadChunkInfo
import com.ismartcoding.plain.data.UploadInfo
import com.ismartcoding.plain.extensions.newFile
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.plain.web.HttpServerManager
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.header
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.toByteArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.text.decodeToString

fun Route.addUploads() {

    post("/upload") {
        val clientId = call.request.header("c-id") ?: ""
        if (clientId.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "c-id header is missing")
            return@post
        }

        val token = HttpServerManager.tokenCache[clientId]
        if (token == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return@post
        }
        try {
            lateinit var info: UploadInfo
            var fileName = ""
            call.receiveMultipart(formFieldLimit = Long.MAX_VALUE).forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        when (part.name) {
                            "info" -> {
                                var requestStr = ""
                                val decryptedBytes = CryptoHelper.chaCha20Decrypt(token, part.provider().toByteArray())
                                if (decryptedBytes != null) {
                                    requestStr = decryptedBytes.decodeToString()
                                }
                                if (requestStr.isEmpty()) {
                                    throw IllegalStateException("Unauthorized")
                                }

                                info = jsonDecode<UploadInfo>(requestStr)
                            }

                            "file" -> {
                                // Strip any path components from the filename to prevent
                                // directory traversal and duplicate-folder bugs (some browsers
                                // include webkitRelativePath in the Content-Disposition filename).
                                fileName = File(part.originalFileName as String).name
                                if (info.isAppFile) {
                                    // Import into content-addressable chat file store for deduplication
                                    val tempFile = File(MainApp.instance.cacheDir, "chat_upload_${System.currentTimeMillis()}_${Thread.currentThread().id}")
                                    tempFile.parentFile?.mkdirs()
                                    FileOutputStream(tempFile).use { fos ->
                                        part.provider().copyTo(fos)
                                        fos.fd.sync()
                                    }
                                    if (info.size > 0 && tempFile.length() != info.size) {
                                        val actual = tempFile.length()
                                        tempFile.delete()
                                        throw IOException("Size mismatch: expected ${info.size}, got $actual")
                                    }
                                    val dFile = AppFileStore.importFile(MainApp.instance, tempFile, part.contentType?.toString() ?: "", deleteSrc = true)
                                    fileName = dFile.id // SHA-256 hash — client forms fid:{hash}
                                } else {
                                    if (info.dir.isEmpty() || fileName.isEmpty()) {
                                        throw IllegalArgumentException("dir or fileName is empty")
                                    }
                                    var destFile = File("${info.dir}/$fileName")
                                    if (destFile.exists()) {
                                        if (info.replace) {
                                            destFile.delete()
                                        } else {
                                            destFile = destFile.newFile()
                                            fileName = destFile.name
                                        }
                                    }
                                    LogCat.d("Upload: ${info.dir}, ${destFile.absolutePath}")
                                    destFile.parentFile?.mkdirs()

                                    // Write to a temp file first, then rename atomically.
                                    // This prevents the file from appearing in listings with a partial size.
                                    val tempFile = File(destFile.parentFile, ".upload_tmp_${System.currentTimeMillis()}_${Thread.currentThread().id}")
                                    try {
                                        FileOutputStream(tempFile).use { fos ->
                                            part.provider().copyTo(fos)
                                            fos.fd.sync()
                                        }
                                        if (info.size > 0 && tempFile.length() != info.size) {
                                            val actual = tempFile.length()
                                            tempFile.delete()
                                            throw IOException("Size mismatch: expected ${info.size}, got $actual")
                                        }
                                        if (!tempFile.renameTo(destFile)) {
                                            tempFile.copyTo(destFile, overwrite = true)
                                            tempFile.delete()
                                        }
                                    } catch (e: Exception) {
                                        tempFile.delete()
                                        throw e
                                    }
                                    MainApp.instance.scanFileByConnection(destFile, null)
                                }
                            }

                            else -> {}
                        }
                    }

                    else -> {
                    }
                }
                part.dispose()
            }
            call.respond(HttpStatusCode.Created, fileName)
        } catch (ex: IllegalStateException) {
            call.respond(HttpStatusCode.Unauthorized)
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respond(HttpStatusCode.BadRequest, ex.message ?: "")
        }
    }

    post("/upload_chunk") {
        val clientId = call.request.header("c-id") ?: ""
        if (clientId.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "c-id header is missing")
            return@post
        }

        val token = HttpServerManager.tokenCache[clientId]
        if (token == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return@post
        }

        try {
            lateinit var chunkInfo: UploadChunkInfo
            var savedSize = 0L

            call.receiveMultipart(formFieldLimit = Long.MAX_VALUE).forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        when (part.name) {
                            "info" -> {
                                var requestStr = ""
                                val decryptedBytes = CryptoHelper.chaCha20Decrypt(token, part.provider().toByteArray())
                                if (decryptedBytes != null) {
                                    requestStr = decryptedBytes.decodeToString()
                                }
                                if (requestStr.isEmpty()) {
                                    throw IllegalStateException("Unauthorized")
                                }

                                chunkInfo = jsonDecode<UploadChunkInfo>(requestStr)
                            }

                            "file" -> {
                                if (chunkInfo.fileId.isEmpty() || chunkInfo.index < 0) {
                                    throw IllegalArgumentException("fileId or index is missing or invalid")
                                }

                                // Create directory in cache dir using file_id as directory name
                                val chunkDir = File(MainApp.instance.filesDir, "upload_tmp/${chunkInfo.fileId}")
                                chunkDir.mkdirs()

                                // Stream chunk directly to disk — never load the entire 5 MB
                                // chunk as a byte array.  With 3 parallel workers, toByteArray()
                                // caused ~30 MB of simultaneous heap allocations which triggered
                                // OOM kills on Huawei/EMUI devices mid-transfer.
                                val chunkFile = File(chunkDir, "chunk_${chunkInfo.index}")
                                val tempFile = File(chunkDir, ".tmp_chunk_${chunkInfo.index}_${System.nanoTime()}")
                                try {
                                    FileOutputStream(tempFile).use { fos ->
                                        part.provider().copyTo(fos)
                                        fos.fd.sync()
                                        savedSize = fos.channel.position()
                                    }

                                    // Verify received size matches expected
                                    if (chunkInfo.size > 0 && savedSize != chunkInfo.size) {
                                        tempFile.delete()
                                        throw IOException("Chunk ${chunkInfo.index} size mismatch: expected ${chunkInfo.size}, received $savedSize")
                                    }

                                    // Atomic rename to final chunk file
                                    if (chunkFile.exists()) chunkFile.delete()
                                    if (!tempFile.renameTo(chunkFile)) {
                                        // renameTo can move the file but still return false
                                        // on some Android file systems. Only use copyTo
                                        // fallback when the source file still exists.
                                        if (tempFile.exists()) {
                                            tempFile.copyTo(chunkFile, overwrite = true)
                                            tempFile.delete()
                                        } else if (!chunkFile.exists()) {
                                            throw IOException("Failed to save chunk ${chunkInfo.index}: rename failed and source file is missing")
                                        }
                                    }

                                    // Post-rename verification: ensure the final chunk
                                    // file has the correct size.
                                    val finalSize = chunkFile.length()
                                    if (chunkInfo.size > 0 && finalSize != chunkInfo.size) {
                                        chunkFile.delete()
                                        throw IOException("Chunk ${chunkInfo.index} final size mismatch: expected ${chunkInfo.size}, saved $finalSize")
                                    }
                                } catch (e: Exception) {
                                    tempFile.delete()
                                    throw e
                                }
                            }

                            else -> {}
                        }
                    }

                    else -> {}
                }
                part.dispose()
            }

            if (savedSize > 0) {
                call.respond(HttpStatusCode.Created, "${chunkInfo.index}:$savedSize")
            } else {
                call.respond(HttpStatusCode.BadRequest, "chunk upload failed")
            }
        } catch (ex: IllegalStateException) {
            call.respond(HttpStatusCode.Unauthorized)
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respond(HttpStatusCode.BadRequest, ex.message ?: "")
        }
    }
}