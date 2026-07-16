package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.OkHttpClientFactory
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.events.UpdateDownloadCompleteEvent
import com.ismartcoding.plain.events.UpdateDownloadFailedEvent
import com.ismartcoding.plain.events.UpdateDownloadProgressEvent
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.preferences.UpdateInfoPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import okhttp3.Request
import java.io.File

private var downloadJob: Job? = null
private val downloadHttpClient by lazy { OkHttpClientFactory.downloadClient() }

actual fun downloadUpdateAsync() {
    downloadJob?.cancel()
    downloadJob = coIO {
        val context = appContext
        val url = UpdateInfoPreference.getValueAsync().downloadUrl
        if (url.isEmpty()) {
            sendEvent(UpdateDownloadFailedEvent())
            return@coIO
        }
        val outputFile = File(context.cacheDir, "plain-update.apk")
        val call = downloadHttpClient.newCall(Request.Builder().url(url).build())
        try {
            val response = call.execute()
            val body = response.body
            val contentLength = body.contentLength()
            var downloaded = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            body.source().use { source ->
                outputFile.outputStream().use { output ->
                    while (true) {
                        ensureActive()
                        val read = source.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val progress = if (contentLength > 0) {
                            ((downloaded * 100) / contentLength).toInt().coerceIn(0, 99)
                        } else 0
                        sendEvent(UpdateDownloadProgressEvent(progress))
                    }
                }
            }
            UpdateInfoPreference.updateAsync { it.copy(downloadedApkPath = outputFile.absolutePath) }
            sendEvent(UpdateDownloadCompleteEvent(outputFile.absolutePath))
        } catch (e: CancellationException) {
            call.cancel()
            outputFile.delete()
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            LogCat.e("APK download failed: $url, ${e.message}")
            outputFile.delete()
            sendEvent(UpdateDownloadFailedEvent())
        }
    }
}

actual fun cancelUpdateDownloadAsync() {
    downloadJob?.cancel()
    downloadJob = null
    coIO { UpdateInfoPreference.updateAsync { it.copy(downloadedApkPath = "") } }
}
