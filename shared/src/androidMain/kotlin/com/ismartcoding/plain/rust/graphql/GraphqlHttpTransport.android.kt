package com.ismartcoding.plain.rust.graphql

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal val peerHttpClient: OkHttpClient by lazy {
    // Paired HTTP bodies and request-bound responses authenticate with the management key over self-signed TLS.
    val trust = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
    }
    val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trust), null) }
    OkHttpClient.Builder()
        .sslSocketFactory(ssl.socketFactory, trust)
        .hostnameVerifier { _, _ -> true }
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
}

internal actual fun createGraphqlHttpTransport(): GraphqlHttpTransport {
    return object : GraphqlHttpTransport {
        override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse = suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(url).header("c-id", clientId)
                .post(body.toRequestBody(contentType.toMediaType())).build()
            val client = if (tlsCertificateSha256 == null) peerHttpClient else {
                createPeerResourceHttpClient(com.ismartcoding.plain.models.ManagedResource(url, tlsCertificateSha256))
                    .newBuilder().callTimeout(30, TimeUnit.SECONDS).build()
            }
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use {
                            val source = it.body.source()
                            check(!source.request(32L * 1024 * 1024 + 1)) { "GraphQL response exceeds 32 MiB" }
                            GraphqlHttpResponse(it.code, source.readByteArray())
                        }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            })
        }
    }
}
