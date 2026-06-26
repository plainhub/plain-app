package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.isOk
import com.ismartcoding.plain.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.api.KtorClientFactory
import com.ismartcoding.plain.api.OkHttpClientFactory
import com.ismartcoding.plain.data.DownloadResult
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

object DownloadHelper {
    suspend fun downloadAsync(url: String, dir: String): DownloadResult = withIO {
        val httpClient = KtorClientFactory.browserClient()
        try {
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

    suspend fun downloadToTempAsync(url: String, tempFile: File): DownloadResult = withIO {
        val httpClient = KtorClientFactory.browserClient()
        try {
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