package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.logcat.LogCat
import kotlin.concurrent.Volatile

/**
 * Kotlin-defined interface that Swift implements to own the SwiftNIO HTTP
 * server lifecycle. Swift registers a single instance via
 * [IosPlatformRegistry.setHttpServerBridge] at app startup; Kotlin calls
 * through the interface when `startHttpServerService()` / `stopHttpServiceAsync()`
 * fire from commonMain.
 *
 * The Swift implementation starts a SwiftNIO `ServerBootstrap` on [httpPort]
 * (and optionally [httpsPort] when TLS is configured). For each HTTP request
 * Swift creates an [IosRequestContext], populates it, calls
 * [processIosHttpRequest] (a top-level suspend fun exposed to Swift via the
 * framework header), then reads the response fields back and writes them to
 * the SwiftNIO channel.
 */
interface IosHttpServerBridge {
    /**
     * Start the SwiftNIO HTTP server.
     * @param httpPort HTTP port to bind
     * @param httpsPort HTTPS port to bind (0 = skip HTTPS)
     * @return true when the server started successfully
     */
    fun start(httpPort: Int, httpsPort: Int): Boolean

    /** Stop the server and release the bound ports. */
    fun stop()

    /** Whether the server is currently accepting connections. */
    fun isRunning(): Boolean
}

/**
 * Swift-implemented transport for a single WebSocket session. Kotlin holds a
 * reference inside [NioWsSession] and calls [sendBinary]/[sendText]/[close]
 * whenever the route handler writes a frame. Swift feeds inbound frames back
 * via [NioWsSession.onBinaryFrame] / [NioWsSession.onClose].
 */
interface IosWsTransport {
    fun sendBinary(data: ByteArray)
    fun sendText(text: String)
    fun close(code: Int, reason: String)
}

/**
 * Singleton registry that lets Swift register platform implementations at app
 * startup. Kotlin code in iosMain reads [httpServerBridge] when commonMain
 * calls `startHttpServerService()` etc.
 */
object IosPlatformRegistry {
    @Volatile
    private var _httpServerBridge: IosHttpServerBridge? = null

    fun setHttpServerBridge(bridge: IosHttpServerBridge) {
        _httpServerBridge = bridge
        LogCat.d("IosPlatformRegistry: HTTP server bridge registered")
    }

    fun httpServerBridge(): IosHttpServerBridge? = _httpServerBridge
}

/**
 * Mutable request/response context that Swift allocates per HTTP request.
 *
 * Swift populates the request side ([method], [path], [remoteHost],
 * [setRequestHeader], [addQueryParam], [setRequestBody]) before calling
 * [processIosHttpRequest]. After the suspend fun returns, Swift reads the
 * response side ([responseStatus], [getResponseHeader], [responseBody]) and
 * writes them to the SwiftNIO channel.
 *
 * Using a single mutable holder avoids passing `Map`/`ByteArray` across the
 * Swift↔Kotlin boundary as function arguments, which keeps the generated
 * Objective-C header simple.
 */
class IosRequestContext(
    val method: String,
    val path: String,
    val remoteHost: String,
) {
    private val requestHeaders = mutableMapOf<String, String>()
    private val queryParams = mutableMapOf<String, MutableList<String>>()
    private var requestBody: ByteArray = ByteArray(0)

    internal var responseStatus: Int = 200
    private val responseHeaders = mutableMapOf<String, String>()
    internal var responseBody: ByteArray = ByteArray(0)
    internal var responseContentType: String? = null
    internal var responseFilePath: String? = null
    internal var responseFileContentType: String? = null
    internal var responseFileContentDisposition: String? = null

    fun setRequestHeader(name: String, value: String) {
        requestHeaders[name] = value
    }

    fun addQueryParam(name: String, value: String) {
        queryParams.getOrPut(name) { mutableListOf() }.add(value)
    }

    fun setRequestBody(data: ByteArray) {
        requestBody = data
    }

    fun getResponseStatus(): Int = responseStatus

    fun getResponseHeaders(): Map<String, String> = responseHeaders.toMap()

    fun getResponseBody(): ByteArray = responseBody

    fun getResponseFilePath(): String? = responseFilePath

    fun getResponseFileContentType(): String? = responseFileContentType

    fun getResponseFileContentDisposition(): String? = responseFileContentDisposition

    // --- internal accessors used by NioHttpCall ---

    internal fun getRequestHeader(name: String): String? = requestHeaders[name]

    internal fun getRequestHeaders(): Map<String, String> = requestHeaders.toMap()

    internal fun getQueryParams(): Map<String, List<String>> =
        queryParams.mapValues { it.value.toList() }

    internal fun getRequestBody(): ByteArray = requestBody

    internal fun setResponseHeader(name: String, value: String) {
        responseHeaders[name] = value
    }

    internal fun setResponseBody(data: ByteArray) {
        responseBody = data
    }

    internal fun setResponseFilePath(path: String?, contentType: String?, disposition: String?) {
        responseFilePath = path
        responseFileContentType = contentType
        responseFileContentDisposition = disposition
    }
}
