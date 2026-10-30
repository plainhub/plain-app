package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.models.ManagedResource
import com.ismartcoding.plain.network.OkHttpClientFactory
import com.ismartcoding.plain.platform.system.cacheDirPath
import com.ismartcoding.plain.rust.graphql.createPeerResourceHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.EOFException
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual suspend fun downloadHttpFile(
    url: String,
    name: String,
    tlsCertificateSha256: String?,
    onProgress: (Long, Long?) -> Unit,
): String = withContext(Dispatchers.IO) { coroutineScope {
    val client = if (tlsCertificateSha256 == null) OkHttpClientFactory.createImageLoaderClient()
        else createPeerResourceHttpClient(ManagedResource(url, tlsCertificateSha256))
    val call = client.newCall(Request.Builder().url(url).build())
    val cancellation = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
        try { awaitCancellation() } finally { call.cancel() }
    }
    try {
    val response = suspendCancellableCoroutine<Response> { continuation ->
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response, onCancellation = { _, received, _ -> received.close() })
                else response.close()
            }
        })
    }
    response.use { received ->
        if (!received.isSuccessful) throw IOException("Download HTTP ${received.code}")
        val directory = File(cacheDirPath(), "resource_downloads")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Unable to create resource download directory")
        val extension = name.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it.length <= 16 && it.all(Char::isLetterOrDigit) }
        val file = File.createTempFile("resource-", extension?.let { ".$it" } ?: ".tmp", directory)
        try {
            val total = received.body.contentLength().takeIf { it >= 0 }
            var downloaded = 0L
            onProgress(0, total)
            received.body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        if (total != null && downloaded > total) throw IOException("Download exceeds declared length")
                        onProgress(downloaded, total)
                    }
                }
            }
            if (total != null && downloaded != total) throw EOFException("Download ended before declared length")
            currentCoroutineContext().ensureActive()
            file.absolutePath
        } catch (error: Throwable) {
            if (!file.delete()) error.addSuppressed(IOException("Unable to remove incomplete resource download"))
            throw error
        }
    }
    } finally { cancellation.cancel() }
} }
