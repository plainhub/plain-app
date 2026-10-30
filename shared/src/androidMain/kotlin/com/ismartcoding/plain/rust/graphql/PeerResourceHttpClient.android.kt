package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.models.ManagedResource
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.util.concurrent.TimeUnit

internal fun createPeerResourceHttpClient(resource: ManagedResource): OkHttpClient {
    val origin = resource.url.toHttpUrl()
    require(origin.isHttps) { "Peer resources require HTTPS" }
    val trust = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = throw CertificateException("Client certificates are unsupported")
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            val leaf = chain.firstOrNull() ?: throw CertificateException("Missing peer certificate")
            val digest = MessageDigest.getInstance("SHA-256").digest(leaf.encoded)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            if (digest != resource.tlsCertificateSha256) throw CertificateException("Peer certificate changed")
        }
    }
    val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trust), null) }
    return OkHttpClient.Builder()
        .sslSocketFactory(ssl.socketFactory, trust)
        .hostnameVerifier { hostname, _ -> hostname == origin.host }
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()
}
