@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.toByteArray
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
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
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi

import coil3.network.NetworkClient
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
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
        val response = CompletableDeferred<NetworkResponse>()

        val delegate = object : NSObject(), NSURLSessionDataDelegateProtocol {
            override fun URLSession(
                session: NSURLSession,
                dataTask: NSURLSessionDataTask,
                didReceiveResponse: NSURLResponse,
                completionHandler: (NSURLSessionResponseDisposition) -> Unit,
            ) {
                val http = didReceiveResponse as? NSHTTPURLResponse
                val status = http?.statusCode?.toInt() ?: 0
                response.complete(NetworkResponse(
                    code = status,
                    headers = imageResponseHeaders(http),
                    body = ImageResponseBody(chunks),
                    delegate = dataTask,
                ))
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
                    if (chunks.trySendBlocking(didReceiveData.toByteArray()).isFailure) dataTask.cancel()
                } catch (error: Exception) {
                    chunks.close(error)
                    response.completeExceptionally(error)
                    dataTask.cancel()
                }
            }

            override fun URLSession(
                session: NSURLSession,
                task: NSURLSessionTask,
                didCompleteWithError: NSError?,
            ) {
                val error = didCompleteWithError?.let { Exception("${it.domain} ${it.code} ${it.localizedDescription}") }
                chunks.close(error)
                if (!response.isCompleted) {
                    response.completeExceptionally(error ?: IllegalStateException("Image request completed without a response"))
                }
            }
        }

        val session = NSURLSession.sessionWithConfiguration(
            imageSessionConfiguration(),
            delegate = delegate,
            delegateQueue = null,
        )
        val task = session.dataTaskWithRequest(nativeRequest)
        return try {
            task.resume()
            val result = response.await()
            LogCat.v("HTTP ${request.method} ${request.url} -> ${result.code}")
            block(result)
        } finally {
            chunks.cancel()
            session.invalidateAndCancel()
        }
    }
}

private fun imageResponseHeaders(response: NSHTTPURLResponse?): NetworkHeaders {
    val headers = NetworkHeaders.Builder()
    response?.allHeaderFields?.forEach { (key, value) ->
        val name = key as? String ?: return@forEach
        headers.add(name, value as? String ?: value.toString())
    }
    return headers.build()
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

private class ImageResponseBody(private val chunks: Channel<ByteArray>) : NetworkResponseBody {
    override suspend fun writeTo(sink: BufferedSink) {
        for (chunk in chunks) sink.write(chunk)
    }

    override suspend fun writeTo(fileSystem: FileSystem, path: Path) {
        fileSystem.write(path) { writeTo(this) }
    }

    override fun close() {
        chunks.cancel()
    }
}
