package com.ismartcoding.plain.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import java.io.IOException
import okhttp3.Callback

internal class OkHttpTestClient(
    private val client: OkHttpClient,
    private val boundedWs: Boolean = false,
) : TestHttpClient {
    override suspend fun request(req: TestHttpRequest): TestHttpResponse {
        val call = client.newCall(req.toOkHttpRequest())
        val response = call.await()
        val headers = response.headers.toMultimap()
        return TestHttpResponse(
            status = HttpStatusCode(response.code),
            url = req.url,
            headers = headers,
            readBody = { withContext(Dispatchers.IO) { response.body.bytes() } },
            onClose = { response.close() },
        )
    }

    override suspend fun <T> webSocket(
        url: String,
        headers: Map<String, String>,
        block: suspend (TestWebSocketSession) -> T,
    ): T {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }
        val session = OkHttpWsSession(client, requestBuilder.build(), boundedWs)
        try {
            return block(session)
        } finally {
            session.close()
        }
    }

    override fun close() {
    }

}

private suspend fun Call.await(): Response =
    suspendCancellableCoroutine { cont ->
        enqueue(
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    cont.resume(response) { _, value, _ -> value.close() }
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
            },
        )
        cont.invokeOnCancellation { cancel() }
    }

internal class OkHttpWsSession(
    client: OkHttpClient,
    request: Request,
    private val bounded: Boolean = false,
) : WebSocketListener(), TestWebSocketSession {
    private val channel = Channel<TestWsFrame>(if (bounded) 4 else Channel.UNLIMITED)
    @Volatile private var closed = false
    private val ws: WebSocket = client.newWebSocket(request, this)

    override val incoming = channel

    override fun onMessage(webSocket: WebSocket, text: String) {
        if (bounded) channel.trySendBlocking(TestWsFrame(text, null)) else channel.trySend(TestWsFrame(text, null))
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        if (bounded) channel.trySendBlocking(TestWsFrame(null, bytes.toByteArray())) else channel.trySend(TestWsFrame(null, bytes.toByteArray()))
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        closed = true
        channel.close()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        closed = true
        channel.close(t)
    }

    override suspend fun sendBinary(data: ByteArray) {
        if (bounded) awaitCapacity()
        check(ws.send(data.toByteString())) { "WebSocket send failed" }
    }

    override suspend fun sendText(text: String) {
        if (bounded) awaitCapacity()
        check(ws.send(text)) { "WebSocket send failed" }
    }

    private suspend fun awaitCapacity() {
        while (!closed && ws.queueSize() > 256 * 1024) delay(5)
        check(!closed) { "WebSocket closed" }
    }

    override fun close() {
        closed = true
        ws.close(1000, null)
        channel.close()
    }
}

internal fun TestHttpRequest.toOkHttpRequest(): Request {
    val builder = Request.Builder().url(url)
    headers.forEach { (k, v) -> builder.header(k, v) }
    val mediaType = contentType?.toMediaTypeOrNull()
    val body: RequestBody? = body?.toRequestBody(mediaType)
    return builder.method(method, body).build()
}

private object TestOkHttpClients {
    val unsafe: OkHttpClient by lazy {
        val trustManager = object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
        }
        val context = javax.net.ssl.SSLContext.getInstance("TLS")
        context.init(emptyArray<javax.net.ssl.KeyManager>(), arrayOf(trustManager), java.security.SecureRandom())
        OkHttpClient.Builder()
            .sslSocketFactory(context.socketFactory, trustManager)
            .hostnameVerifier { hostname, _ -> com.ismartcoding.plain.lib.helpers.NetworkHelper.isLocalNetworkAddress(hostname) }
            .build()
    }
    val host: OkHttpClient by lazy {
        unsafe.newBuilder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
    }
}

fun createTestHttpClient(spec: TestHttpClientSpec): TestHttpClient = when (spec) {
    is TestHttpClientSpec.Local -> OkHttpTestClient(
        TestOkHttpClients.unsafe.newBuilder()
            .connectTimeout(spec.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(spec.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(spec.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build(),
    )
    TestHttpClientSpec.HttpHost -> OkHttpTestClient(TestOkHttpClients.host, boundedWs = true)
}
