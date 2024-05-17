package com.ismartcoding.plain.helpers

import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.isOk
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.data.DownloadResult
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

object DownloadHelper {
    suspend fun downloadAsync(url: String, dir: String): DownloadResult {
        val httpClient = HttpClientManager.browserClient()
        return try {
            val r = httpClient.get(url)
            if (r.isOk()) {
                File(dir).mkdirs()
                var path = "$dir/${CryptoHelper.sha1(url.toByteArray())}"
                val extension = url.getFilenameExtension()
                if (extension.isNotEmpty()) {
                    path += ".$extension"
                }
                val file = File(path)
                file.createNewFile()
                r.bodyAsChannel().copyAndClose(file.writeChannel())
                MainApp.instance.scanFileByConnection(file, null)
                DownloadResult(path, true)
            } else {
                DownloadResult("", false, r.toString())
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
            ex.printStackTrace()
            DownloadResult("", false, ex.toString())
        }
    }

    suspend fun downloadToTempAsync(url: String, tempFile: File): DownloadResult {
        val httpClient = HttpClientManager.browserClient()
        return try {
            val r = httpClient.get(url)
            if (r.isOk()) {
                r.bodyAsChannel().copyAndClose(tempFile.writeChannel())
                DownloadResult(tempFile.absolutePath, true)
            } else {
                DownloadResult("", false, r.toString())
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
            ex.printStackTrace()
            DownloadResult("", false, ex.toString())
        }
    }
}