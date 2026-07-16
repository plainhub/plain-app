package com.ismartcoding.plain.web.routes

import com.ismartcoding.plain.data.UploadChunkInfo
import com.ismartcoding.plain.data.UploadInfo
import com.ismartcoding.plain.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.createFileSink
import com.ismartcoding.plain.platform.createTempFilePath
import com.ismartcoding.plain.platform.deleteFileAt
import com.ismartcoding.plain.platform.ensureParentDir
import com.ismartcoding.plain.platform.getNewPath
import com.ismartcoding.plain.platform.getUploadTmpDirPath
import com.ismartcoding.plain.platform.importAppFile
import com.ismartcoding.plain.platform.renameFileAtomic
import com.ismartcoding.plain.platform.scanFiles
import com.ismartcoding.plain.platform.statFile
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.http.HttpMultipartPart
import com.ismartcoding.plain.web.http.HttpRouter
import com.ismartcoding.plain.web.http.HttpStatus
import kotlin.random.Random

/**
 * `/upload` and `/upload_chunk` — multipart file upload endpoints shared
 * between Android (Ktor) and iOS (SwiftNIO future). Both endpoints verify the
 * `c-id` header against the in-memory token cache and decrypt the `info` part
 * with ChaCha20 before any file content is written.
 *
 * The chunk endpoint streams each chunk directly to disk to avoid large heap
 * allocations when multiple workers upload in parallel (the previous
 * `toByteArray()` approach triggered OOM kills on Huawei/EMUI devices).
 */
fun HttpRouter.addUploadRoutes() {
    post("/upload") { call ->
        val clientId = call.header("c-id") ?: ""
        if (clientId.isEmpty()) {
            call.respondText("c-id header is missing", status = HttpStatus.BAD_REQUEST)
            return@post
        }
        val token = HttpServerManager.tokenCache[clientId]
        if (token == null) {
            call.respondNoBody(HttpStatus.UNAUTHORIZED)
            return@post
        }

        try {
            var info: UploadInfo? = null
            var fileName = ""
            call.handleMultipart { part ->
                when (part.name) {
                    "info" -> {
                        val decryptedBytes = chaCha20Decrypt(token, part.readBytes())
                        val requestStr = decryptedBytes?.decodeToString() ?: ""
                        if (requestStr.isEmpty()) {
                            throw IllegalStateException("Unauthorized")
                        }
                        info = jsonDecode<UploadInfo>(requestStr)
                    }

                    "file" -> {
                        val currentInfo = info
                            ?: throw IllegalStateException("info part must precede file part")
                        // Strip path components (some browsers include
                        // webkitRelativePath in Content-Disposition filename).
                        val rawName = part.originalFileName ?: ""
                        fileName = rawName.substringAfterLast('/').substringAfterLast('\\')

                        if (currentInfo.isAppFile) {
                            fileName = handleAppFileUpload(part, currentInfo, fileName)
                        } else {
                            if (currentInfo.dir.isEmpty() || fileName.isEmpty()) {
                                throw IllegalArgumentException("dir or fileName is empty")
                            }
                            fileName = handleRegularFileUpload(part, currentInfo, fileName)
                        }
                    }
                }
            }
            call.respondText(fileName, status = HttpStatus.CREATED)
        } catch (ex: IllegalStateException) {
            call.respondNoBody(HttpStatus.UNAUTHORIZED)
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respondText(ex.message ?: "", status = HttpStatus.BAD_REQUEST)
        }
    }

    post("/upload_chunk") { call ->
        val clientId = call.header("c-id") ?: ""
        if (clientId.isEmpty()) {
            call.respondText("c-id header is missing", status = HttpStatus.BAD_REQUEST)
            return@post
        }
        val token = HttpServerManager.tokenCache[clientId]
        if (token == null) {
            call.respondNoBody(HttpStatus.UNAUTHORIZED)
            return@post
        }

        try {
            var chunkInfo: UploadChunkInfo? = null
            var savedSize = 0L

            call.handleMultipart { part ->
                when (part.name) {
                    "info" -> {
                        val decryptedBytes = chaCha20Decrypt(token, part.readBytes())
                        val requestStr = decryptedBytes?.decodeToString() ?: ""
                        if (requestStr.isEmpty()) {
                            throw IllegalStateException("Unauthorized")
                        }
                        chunkInfo = jsonDecode<UploadChunkInfo>(requestStr)
                    }

                    "file" -> {
                        val currentInfo = chunkInfo
                            ?: throw IllegalStateException("info part must precede file part")
                        if (currentInfo.fileId.isEmpty() || currentInfo.index < 0) {
                            throw IllegalArgumentException("fileId or index is missing or invalid")
                        }
                        savedSize = handleChunkUpload(part, currentInfo)
                    }
                }
            }

            if (savedSize > 0) {
                val currentInfo = chunkInfo
                val index = currentInfo?.index ?: 0
                call.respondText("$index:$savedSize", status = HttpStatus.CREATED)
            } else {
                call.respondText("chunk upload failed", status = HttpStatus.BAD_REQUEST)
            }
        } catch (ex: IllegalStateException) {
            call.respondNoBody(HttpStatus.UNAUTHORIZED)
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respondText(ex.message ?: "", status = HttpStatus.BAD_REQUEST)
        }
    }
}

/**
 * Import an uploaded file into the content-addressable AppFileStore so chat
 * files are deduplicated by hash. Returns the "{hash}.{ext}" suffix used to
 * build `fid:` URIs.
 */
private suspend fun handleAppFileUpload(
    part: HttpMultipartPart,
    info: UploadInfo,
    fileName: String,
): String {
    val tempFilePath = createTempFilePath("chat_upload")
    ensureParentDir(tempFilePath)
    val sink = createFileSink(tempFilePath)
    try {
        part.copyTo(sink)
    } catch (e: Exception) {
        deleteFileAt(tempFilePath)
        throw e
    }

    val actualSize = statFile(tempFilePath)?.size ?: 0L
    if (info.size > 0 && actualSize != info.size) {
        deleteFileAt(tempFilePath)
        throw Exception("Size mismatch: expected ${info.size}, got $actualSize")
    }

    val fidSuffix = importAppFile(tempFilePath, part.contentType ?: "", deleteSrc = true)
    return fidSuffix ?: throw Exception("Failed to import app file")
}

/**
 * Write an uploaded file to [info.dir]/[fileName], handling replace vs rename
 * on collision and verifying the received size matches `info.size`.
 * Returns the final file name (may differ from the input when a "(1)" suffix
 * was appended to avoid overwriting an existing file).
 */
private suspend fun handleRegularFileUpload(
    part: HttpMultipartPart,
    info: UploadInfo,
    fileName: String,
): String {
    var destPath = "${info.dir}/$fileName"
    val existingStat = statFile(destPath)
    var finalFileName = fileName
    if (existingStat != null) {
        if (info.replace) {
            deleteFileAt(destPath)
        } else {
            destPath = getNewPath(destPath)
            finalFileName = destPath.substringAfterLast('/')
        }
    }
    LogCat.d("Upload: ${info.dir}, $destPath")
    ensureParentDir(destPath)

    val tempFilePath = "${info.dir}/.upload_tmp_${TimeHelper.nowMillis()}_${Random.nextInt()}"
    val tempSink = createFileSink(tempFilePath)
    try {
        part.copyTo(tempSink)
    } catch (e: Exception) {
        deleteFileAt(tempFilePath)
        throw e
    }

    val actualSize = statFile(tempFilePath)?.size ?: 0L
    if (info.size > 0 && actualSize != info.size) {
        deleteFileAt(tempFilePath)
        throw Exception("Size mismatch: expected ${info.size}, got $actualSize")
    }

    if (!renameFileAtomic(tempFilePath, destPath)) {
        deleteFileAt(tempFilePath)
        throw Exception("Failed to move uploaded file into place")
    }

    scanFiles(arrayOf(destPath))
    return finalFileName
}

/**
 * Save an uploaded chunk to the per-fileId upload temp directory using an
 * atomic temp-then-rename pattern. Verifies the received size matches the
 * expected size. Returns the number of bytes saved.
 *
 * The chunk directory lives under the platform's upload tmp path
 * (`upload_tmp/{fileId}/chunk_{index}`).
 */
private suspend fun handleChunkUpload(
    part: HttpMultipartPart,
    chunkInfo: UploadChunkInfo,
): Long {
    val chunkDir = "${getUploadTmpDirPath()}/${chunkInfo.fileId}"
    ensureParentDir("$chunkDir/.placeholder")
    val chunkFilePath = "$chunkDir/chunk_${chunkInfo.index}"
    val tempFilePath = "$chunkDir/.tmp_chunk_${chunkInfo.index}_${TimeHelper.nowMillis()}_${Random.nextInt()}"

    val tempSink = createFileSink(tempFilePath)
    var savedSize = 0L
    try {
        part.copyTo(tempSink)
        savedSize = statFile(tempFilePath)?.size ?: 0L

        if (chunkInfo.size > 0 && savedSize != chunkInfo.size) {
            deleteFileAt(tempFilePath)
            throw Exception(
                "Chunk ${chunkInfo.index} size mismatch: expected ${chunkInfo.size}, received $savedSize"
            )
        }

        // Replace any existing chunk with the new one.
        deleteFileAt(chunkFilePath)
        if (!renameFileAtomic(tempFilePath, chunkFilePath)) {
            // renameTo can fail but still move the file on some Android file
            // systems. Verify by checking the final chunk file size.
            val finalSize = statFile(chunkFilePath)?.size ?: -1L
            if (finalSize == -1L) {
                throw Exception(
                    "Failed to save chunk ${chunkInfo.index}: rename failed and source file is missing"
                )
            }
            savedSize = finalSize
        }

        // Post-rename verification: ensure final chunk file has the expected size.
        val finalSize = statFile(chunkFilePath)?.size ?: 0L
        if (chunkInfo.size > 0 && finalSize != chunkInfo.size) {
            deleteFileAt(chunkFilePath)
            throw Exception(
                "Chunk ${chunkInfo.index} final size mismatch: expected ${chunkInfo.size}, saved $finalSize"
            )
        }
        savedSize = finalSize
    } catch (e: Exception) {
        deleteFileAt(tempFilePath)
        throw e
    }

    return savedSize
}
