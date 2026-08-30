@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.httpLogSink
import com.ismartcoding.plain.lib.toNSData
import kotlinx.cinterop.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSError
import platform.Foundation.NSHTTPCookieStorage
import platform.Foundation.NSURL
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLRequestReloadIgnoringLocalAndRemoteCacheData
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionResponseAllow
import platform.Foundation.NSURLSessionResponseDisposition
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionWebSocketCloseCodeNormalClosure
import platform.Foundation.NSURLSessionWebSocketMessage
import platform.Foundation.NSURLSessionWebSocketTask
import platform.Foundation.create
import platform.Foundation.setValue
import platform.Foundation.setHTTPMethod
import platform.Foundation.setAllHTTPHeaderFields
import platform.Foundation.setHTTPBody
import platform.Foundation.serverTrust
import platform.darwin.NSObject
import platform.Foundation.NSURLSessionWebSocketDelegateProtocol
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Job
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * NSURLSession-backed [PlainHttpClient] for iOS. This is the native
 * implementation that replaced the temporary Darwin-based wrapper, so no HTTP
 * client from ktor is needed on any target.
 *
 * A fresh session is created per request/connection: it lets each request own
 * its delegate (chunk sink + TLS trust decision) without sharing mutable state
 * across NSURLSession callback threads.
 */
internal class IosPlainClient(
    private val spec: PlainHttpClientSpec,
) : PlainHttpClient {
    override val cryptoKey: ByteArray? get() = (spec as? PlainHttpClientSpec.Crypto)?.keyBytes

    private val trustAll: Boolean
        get() = spec !is PlainHttpClientSpec.Default && spec !is PlainHttpClientSpec.Browser

    override suspend fun request(req: PlainRequest): PlainResponse {
        // Bounded so a slow consumer blocks the session delegate thread, which
        // pushes back on NSURLSession's internal buffering instead of holding
        // the whole body in memory.
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

            override fun URLSession(
                session: NSURLSession,
                dataTask: NSURLSessionDataTask,
                didReceiveData: NSData,
            ) {
                // Closed channel (release raced with delivery) must not escape
                // into the delegate callback.
                runCatching { chunks.trySendBlocking(didReceiveData.toByteArray()) }
            }

            override fun URLSession(
                session: NSURLSession,
                task: NSURLSessionTask,
                didCompleteWithError: NSError?,
            ) {
                val error = didCompleteWithError
                if (error != null) {
                    val ex = Exception("${error.domain} ${error.code} ${error.localizedDescription}")
                    failure = ex
                    chunks.close(ex)
                } else {
                    chunks.close()
                }
                headerContinuation?.invoke()
            }

            override fun URLSession(
                session: NSURLSession,
                didReceiveChallenge: NSURLAuthenticationChallenge,
                completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
            ) {
                if (trustAll && didReceiveChallenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
                    val trust = didReceiveChallenge.protectionSpace.serverTrust
                    if (trust != null) {
                        completionHandler(NSURLSessionAuthChallengeUseCredential, NSURLCredential.create(trust))
                        return
                    }
                }
                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
            }
        }

        val session = NSURLSession.sessionWithConfiguration(
            configurationFor(spec),
            delegate = delegate,
            delegateQueue = null,
        )
        val task = session.dataTaskWithRequest(req.toNSRequest(spec))
        val channel = ByteChannel(autoFlush = true)
        val scope = CoroutineScope(IODispatcher + SupervisorJob())
        val pump: Job = scope.launch {
            try {
                for (chunk in chunks) channel.writeFully(chunk)
                channel.flush()
                channel.close()
            } catch (e: Exception) {
                channel.close(e)
            }
        }

        // Close `chunks` first: it unblocks a delegate thread parked on a full
        // bounded channel so the task/session teardown can proceed.
        val released = { chunks.close(); pump.cancel(); task.cancel(); session.invalidateAndCancel() }
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
        if (spec is PlainHttpClientSpec.Browser) {
            httpLogSink.log("HTTP ${req.method} ${req.url} -> $status")
        }
        return PlainResponse(HttpStatusCode(status), req.url, responseHeaders, channel, onClose = released)
    }

    override suspend fun <T> webSocket(
        url: String,
        headers: Map<String, String>,
        block: suspend (PlainWebSocketSession) -> T,
    ): T {
        val request = NSMutableURLRequest(uRL = NSURL.URLWithString(url)!!)
        headers.forEach { (k, v) -> request.setValue(v, forHTTPHeaderField = k) }
        val delegate = object : NSObject(), NSURLSessionWebSocketDelegateProtocol {
            override fun URLSession(
                session: NSURLSession,
                didReceiveChallenge: NSURLAuthenticationChallenge,
                completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
            ) {
                if (trustAll && didReceiveChallenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
                    val trust = didReceiveChallenge.protectionSpace.serverTrust
                    if (trust != null) {
                        completionHandler(NSURLSessionAuthChallengeUseCredential, NSURLCredential.create(trust))
                        return
                    }
                }
                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
            }
        }
        val session = NSURLSession.sessionWithConfiguration(
            configurationFor(spec),
            delegate = delegate,
            delegateQueue = null,
        )
        val task = session.webSocketTaskWithRequest(request)
        task.resume()
        val wsSession = IosWebSocketSession(task)
        wsSession.startReading()
        try {
            return block(wsSession)
        } finally {
            wsSession.close()
            session.invalidateAndCancel()
        }
    }

    override fun close() {
        // Sessions are per-request and invalidated when the response is released.
    }
}

private class IosWebSocketSession(
    private val task: NSURLSessionWebSocketTask,
) : PlainWebSocketSession {
    override val incoming = Channel<PlainWsFrame>(Channel.UNLIMITED)
    private val scope = CoroutineScope(IODispatcher + SupervisorJob())

    fun startReading() {
        scope.launch {
            try {
                while (true) {
                    val message = receiveMessage() ?: break
                    val frame = if (message.data != null) {
                        PlainWsFrame(null, message.data!!.toByteArray())
                    } else {
                        PlainWsFrame(message.string, null)
                    }
                    incoming.trySend(frame)
                }
                incoming.close()
            } catch (e: Exception) {
                incoming.close(e)
            }
        }
        // Keep-alive: NSURLSession has no ping interval, so send one manually.
        scope.launch {
            while (true) {
                delay(15_000)
                task.sendPingWithPongReceiveHandler { }
            }
        }
    }

    private suspend fun receiveMessage(): NSURLSessionWebSocketMessage? =
        suspendCancellableCoroutine { cont ->
            task.receiveMessageWithCompletionHandler { message, error ->
                if (error != null) {
                    if (cont.isActive) {
                        cont.resumeWithException(
                            Exception("${error.domain} ${error.code} ${error.localizedDescription}"),
                        )
                    }
                } else if (cont.isActive) {
                    cont.resume(message)
                }
            }
            cont.invokeOnCancellation { task.cancel() }
        }

    override suspend fun sendBinary(data: ByteArray) {
        sendMessage(NSURLSessionWebSocketMessage(data.toNSData()))
    }

    override suspend fun sendText(text: String) {
        sendMessage(NSURLSessionWebSocketMessage(text))
    }

    private suspend fun sendMessage(message: NSURLSessionWebSocketMessage) =
        suspendCancellableCoroutine<Unit> { cont ->
            task.sendMessage(message) { error ->
                if (cont.isActive) {
                    if (error != null) {
                        cont.resumeWithException(
                            Exception("${error.domain} ${error.code} ${error.localizedDescription}"),
                        )
                    } else {
                        cont.resume(Unit)
                    }
                }
            }
        }

    override fun close() {
        task.cancelWithCloseCode(NSURLSessionWebSocketCloseCodeNormalClosure, null)
        incoming.close()
        scope.cancel()
    }
}

private fun PlainRequest.toNSRequest(spec: PlainHttpClientSpec): NSMutableURLRequest {
    val request = NSMutableURLRequest(uRL = NSURL.URLWithString(url)!!)
    request.setHTTPMethod(method)
    request.setCachePolicy(NSURLRequestReloadIgnoringLocalAndRemoteCacheData)
    val headerMap = headers.toMutableMap()
    if (spec is PlainHttpClientSpec.Browser && headerMap.keys.none { it.equals("User-Agent", true) }) {
        headerMap["User-Agent"] = BROWSER_USER_AGENT
    }
    request.setAllHTTPHeaderFields(headerMap as Map<Any?, *>)
    contentType?.let { request.setValue(it, forHTTPHeaderField = "Content-Type") }
    body?.let { request.setHTTPBody(it.toNSData()) }
    if (spec is PlainHttpClientSpec.Browser) {
        httpLogSink.log("HTTP request: $method $url headers=$headerMap")
    }
    return request
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

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size <= 0) return ByteArray(0)
    return bytes?.readBytes(size) ?: ByteArray(0)
}

private fun configurationFor(spec: PlainHttpClientSpec): NSURLSessionConfiguration {
    val config = if (spec is PlainHttpClientSpec.Browser) {
        NSURLSessionConfiguration.defaultSessionConfiguration.apply {
            setHTTPCookieStorage(NSHTTPCookieStorage.sharedHTTPCookieStorage)
        }
    } else {
        NSURLSessionConfiguration.ephemeralSessionConfiguration
    }
    when (spec) {
        PlainHttpClientSpec.Default -> {
            config.setTimeoutIntervalForRequest(PlainHttpTimeouts.DEFAULT_MS / 1000.0)
            config.setTimeoutIntervalForResource(60.0)
        }

        PlainHttpClientSpec.Browser -> {
            config.setTimeoutIntervalForRequest(PlainHttpTimeouts.BROWSER_MS / 1000.0)
            config.setTimeoutIntervalForResource(60.0)
        }

        PlainHttpClientSpec.Unsafe -> {
            config.setTimeoutIntervalForRequest(30.0)
            config.setTimeoutIntervalForResource(300.0)
        }

        PlainHttpClientSpec.Download -> {
            config.setTimeoutIntervalForRequest(120.0)
            config.setTimeoutIntervalForResource(3600.0)
        }

        PlainHttpClientSpec.PeerStatus -> {
            // Long-lived WebSocket: no request timeout, keep-alive handled by ping.
            config.setTimeoutIntervalForRequest(3600.0)
            config.setTimeoutIntervalForResource(3600.0 * 24)
        }

        is PlainHttpClientSpec.Crypto -> {
            config.setTimeoutIntervalForRequest(spec.timeoutSeconds.toDouble())
            config.setTimeoutIntervalForResource(spec.timeoutSeconds.toDouble() * 6)
        }
    }
    return config
}

actual fun createPlainHttpClient(spec: PlainHttpClientSpec): PlainHttpClient = IosPlainClient(spec)
