@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.ismartcoding.plain.platform.system

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSError
import platform.Foundation.NSHTTPCookieStorage
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequestReloadIgnoringLocalAndRemoteCacheData
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionResponseAllow
import platform.Foundation.NSURLSessionResponseDisposition
import platform.Foundation.NSURLSessionTask
import platform.Foundation.create
import platform.Foundation.setHTTPMethod
import platform.Foundation.setAllHTTPHeaderFields
import platform.darwin.NSObject
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeFully
import kotlin.coroutines.resume
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi

import coil3.network.NetworkClient
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import io.ktor.utils.io.readAvailable
import okio.BufferedSink
import okio.FileSystem
import okio.Path

internal class IosImageNetworkClient(private val tlsCertificateSha256: String? = null) : NetworkClient {
    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (NetworkResponse) -> T,
    ): T {
        val nativeRequest = request.toImageRequest()
        if (tlsCertificateSha256 != null) require(nativeRequest.URL?.scheme == "https") { "Peer resources require HTTPS" }
        val chunks = Channel<ByteArray>(capacity = 8)
        var status = 0
        var responseHeaders: Map<String, List<String>> = emptyMap()
        var headerContinuation: (() -> Unit)? = null
        var failure: Throwable? = null

        val delegate = object : NSObject(), NSURLSessionDataDelegateProtocol {
            override fun URLSession(
                session: NSURLSession,
                dataTask: NSURLSessionDataTask,
                didReceiveResponse: NSURLResponse,
                completionHandler: (NSURLSessionResponseDisposition) -> Unit,
            ) {
                val http = didReceiveResponse as? NSHTTPURLResponse
                status = http?.statusCode?.toInt() ?: 0
                responseHeaders = headerMap(http)
                headerContinuation?.invoke()
                completionHandler(NSURLSessionResponseAllow)
            }

            override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didReceiveChallenge: NSURLAuthenticationChallenge, completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit) {
                val pin = tlsCertificateSha256
                if (pin == null) completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
                else {
                    val credential = com.ismartcoding.plain.rust.graphql.peerResourceCredential(didReceiveChallenge, nativeRequest.URL?.host, pin)
                    if (credential != null) completionHandler(NSURLSessionAuthChallengeUseCredential, credential)
                    else completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
                }
            }
            override fun URLSession(session: NSURLSession, task: NSURLSessionTask, willPerformHTTPRedirection: NSHTTPURLResponse, newRequest: platform.Foundation.NSURLRequest, completionHandler: (platform.Foundation.NSURLRequest?) -> Unit) {
                completionHandler(if (tlsCertificateSha256 == null) newRequest else null)
            }

            override fun URLSession(
                session: NSURLSession,
                dataTask: NSURLSessionDataTask,
                didReceiveData: NSData,
            ) {
                try {
                    chunks.trySendBlocking(didReceiveData.toByteArray())
                } catch (error: Exception) {
                    failure = error
                    chunks.close(error)
                    dataTask.cancel()
                    headerContinuation?.invoke()
                }
            }

            override fun URLSession(
                session: NSURLSession,
                task: NSURLSessionTask,
                didCompleteWithError: NSError?,
            ) {
                val error = didCompleteWithError
                if (error != null) {
                    val ex = Exception("${error.domain} ${error.code} ${error.localizedDescription}")
                    failure = failure ?: ex
                    chunks.close(failure)
                } else {
                    chunks.close()
                }
                headerContinuation?.invoke()
            }

        }

        val session = NSURLSession.sessionWithConfiguration(
            imageSessionConfiguration(),
            delegate = delegate,
            delegateQueue = null,
        )
        val task = session.dataTaskWithRequest(nativeRequest)
        val channel = ByteChannel(autoFlush = true)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                for (chunk in chunks) channel.writeFully(chunk)
                channel.flush()
                channel.close()
            } catch (e: Exception) {
                channel.close(e)
            }
        }

        val released = {
            chunks.close()
            channel.cancel(null)
            scope.cancel()
            task.cancel()
            session.invalidateAndCancel()
        }
        try {
            suspendCancellableCoroutine<Unit> { cont ->
                headerContinuation = { if (cont.isActive) cont.resume(Unit) }
                task.resume()
                cont.invokeOnCancellation { task.cancel() }
            }
        } catch (e: Exception) {
            released()
            throw e
        }
        failure?.let { released(); throw it }
        LogCat.v("HTTP ${request.method} ${request.url} -> $status")
        return try {
            block(NetworkResponse(
                code = status,
                headers = responseHeaders.entries.fold(NetworkHeaders.Builder()) { builder, (name, values) ->
                    builder.apply { values.forEach { add(name, it) } }
                }.build(),
                body = ImageResponseBody(channel),
                delegate = task,
            ))
        } finally {
            released()
        }
    }
}

private fun headerMap(response: NSHTTPURLResponse?): Map<String, List<String>> {
    if (response == null) return emptyMap()
    val fields = response.allHeaderFields
    val out = HashMap<String, List<String>>()
    fields.forEach { (key, value) ->
        val name = key as? String ?: return@forEach
        out[name] = listOf(value as? String ?: value.toString())
    }
    return out
}

private fun NetworkRequest.toImageRequest(): NSMutableURLRequest {
    val request = NSMutableURLRequest(uRL = requireNotNull(NSURL.URLWithString(url)))
    request.setHTTPMethod(method)
    request.setCachePolicy(NSURLRequestReloadIgnoringLocalAndRemoteCacheData)
    val fields = headers.asMap().mapValues { (_, values) -> values.lastOrNull().orEmpty() }.toMutableMap()
    if (fields.keys.none { it.equals("User-Agent", true) }) fields["User-Agent"] = IMAGE_USER_AGENT
    request.setAllHTTPHeaderFields(fields as Map<Any?, *>)
    return request
}

private fun imageSessionConfiguration(): NSURLSessionConfiguration =
    NSURLSessionConfiguration.defaultSessionConfiguration.apply {
        setHTTPCookieStorage(NSHTTPCookieStorage.sharedHTTPCookieStorage)
        setTimeoutIntervalForRequest(20.0)
        setTimeoutIntervalForResource(60.0)
    }

private const val IMAGE_USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

private class ImageResponseBody(private val channel: ByteReadChannel) : NetworkResponseBody {
    override suspend fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val size = channel.readAvailable(buffer)
            if (size <= 0) break
            sink.write(buffer, 0, size)
        }
    }

    override suspend fun writeTo(fileSystem: FileSystem, path: Path) {
        fileSystem.write(path) { writeTo(this) }
    }

    override fun close() {
        channel.cancel(null)
    }
}
