package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.data.DownloadResult
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.PlainResponse
import com.ismartcoding.plain.platform.copyTo
import com.ismartcoding.plain.platform.createBrowserHttpClient
import com.ismartcoding.plain.platform.get
import com.ismartcoding.plain.platform.sha1
import java.io.File
import java.io.FileOutputStream

object DownloadHelper {
    suspend fun downloadAsync(url: String, dir: String): DownloadResult = withIO {
        val httpClient = createBrowserHttpClient()
        try {
            val r = httpClient.get(url)
            r.use {
                if (it.isOk()) {
                    File(dir).mkdirs()
                    var path = "$dir/${sha1(url.toByteArray())}"
                    val extension = url.getFilenameExtension()
                    if (extension.isNotEmpty()) {
                        path += ".$extension"
                    }
                    val file = File(path)
                    file.createNewFile()
                    it.writeBodyToFile(file)
                    appContext.scanFileByConnection(file, null)
                    DownloadResult(path, true)
                } else {
                    DownloadResult("", false, "HTTP ${it.status}")
                }
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
            ex.printStackTrace()
            DownloadResult("", false, ex.toString())
        }
    }

    suspend fun downloadToTempAsync(url: String, tempFile: File): DownloadResult = withIO {
        val httpClient = createBrowserHttpClient()
        try {
            val r = httpClient.get(url)
            r.use {
                if (it.isOk()) {
                    it.writeBodyToFile(tempFile)
                    DownloadResult(tempFile.absolutePath, true)
                } else {
                    DownloadResult("", false, "HTTP ${it.status}")
                }
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
            ex.printStackTrace()
            DownloadResult("", false, ex.toString())
        }
    }

    private suspend fun PlainResponse.writeBodyToFile(file: File) {
        FileOutputStream(file).use { out ->
            channel.copyTo { buffer, length -> out.write(buffer, 0, length) }
        }
    }
}
