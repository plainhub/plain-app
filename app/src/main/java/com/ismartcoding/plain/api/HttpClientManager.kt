package com.ismartcoding.plain.api

import android.util.Base64
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.PhoneHelper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.headers
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.KeyManager
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object HttpClientManager {
    fun browserClient() =
        HttpClient(CIO) {
            BrowserUserAgent()
            install(Logging) {
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            LogCat.v(message)
                        }
                    }
                level = LogLevel.HEADERS
            }
            install(HttpCookies)
            install(HttpTimeout) {
                requestTimeoutMillis = HttpApiTimeout.BROWSER_SECONDS * 1000L
            }
            headers {
                set("accept", "*/*")
            }
        }

    fun downloadClient() = HttpClient(CIO)

    fun httpClient() =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = HttpApiTimeout.MEDIUM_SECONDS * 1000L
            }
            install(WebSockets)
        }

    private fun OkHttpClient.Builder.ignoreAllSSLErrors(): OkHttpClient.Builder {
        val naiveTrustManager =
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

                override fun checkClientTrusted(
                    certs: Array<X509Certificate>,
                    authType: String,
                ) = Unit

                override fun checkServerTrusted(
                    certs: Array<X509Certificate>,
                    authType: String,
                ) = Unit
            }

        val insecureSocketFactory =
            SSLContext.getInstance("TLSv1.2").apply {
                val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
                // Avoid triggering default KeyManager lookup (which can require BKS on some devices)
                init(arrayOf<KeyManager>(), trustAllCerts, SecureRandom())
            }.socketFactory

        sslSocketFactory(insecureSocketFactory, naiveTrustManager)
        hostnameVerifier { _, _ -> true }
        return this
    }

    fun createCryptoHttpClient(
        token: String,
        timeout: Int,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val requestBody = request.body!!
                val requestBodyStr = bodyToString(requestBody)
                LogCat.d("[Request] $requestBodyStr")
                val response =
                    chain.proceed(
                        request.newBuilder()
                            .addHeader("c-id", TempData.clientId)
                            .addHeader("c-platform", "android")
                            .addHeader(
                                "c-name",
                                Base64.encodeToString(PhoneHelper.getDeviceName(MainApp.instance).toByteArray(), Base64.NO_WRAP),
                            )
                            .addHeader("c-version", MainApp.getAppVersion())
                            .post(CryptoHelper.chaCha20Encrypt(token, requestBodyStr).toRequestBody(requestBody.contentType()))
                            .build(),
                    )
                val responseBody = response.body!!
                val decryptedBytes = CryptoHelper.chaCha20Decrypt(token, responseBody.bytes())
                if (decryptedBytes != null) {
                    val json = decryptedBytes.decodeToString()
                    LogCat.d("[Response] $json")
                    return@addInterceptor response.newBuilder().body(json.toResponseBody(responseBody.contentType())).build()
                }
                response.newBuilder().build()
            }
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .ignoreAllSSLErrors()
            .build()
    }

    fun createUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS")
        // Avoid default KeyManager to prevent KeyStoreException: BKS not found
        sslContext.init(arrayOf<KeyManager>(), trustAllCerts, SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { hostname, _ ->
                NetworkHelper.isLocalNetworkAddress(hostname)
            }
            .build()
    }

    private fun bodyToString(request: RequestBody): String {
        val buffer = okio.Buffer()
        request.writeTo(buffer)
        return buffer.readUtf8()
    }
}
