package com.ismartcoding.plain.ui.base.coil

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.util.DebugLogger
import com.ismartcoding.plain.activityManager
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

fun newImageLoader(context: PlatformContext): ImageLoader {
    val memoryPercent = if (activityManager.isLowRamDevice) 0.25 else 0.75
    
    // Create unsafe OkHttp client for SSL bypass
    val unsafeOkHttpClient = createUnsafeOkHttpClient()
    
    return ImageLoader.Builder(context)
        .components {
            add(SvgDecoder.Factory(true))
            add(AnimatedImageDecoder.Factory())
            add(ThumbnailDecoder.Factory())
            add(OkHttpNetworkFetcherFactory(unsafeOkHttpClient))
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, percent = memoryPercent)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache").absoluteFile)
                .maxSizePercent(1.0)
                .build()
        }
        .crossfade(100)
        .allowRgb565(true)
        .logger(DebugLogger())
        .build()
}

private fun createUnsafeOkHttpClient(): OkHttpClient {
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustAllCerts, SecureRandom())
    val sslSocketFactory = sslContext.socketFactory
    
    return OkHttpClient.Builder()
        .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { hostname, _ -> 
            // 只对局域网 IP 跳过 SSL 验证
            isLocalNetworkAddress(hostname)
        }
        .build()
}

private fun isLocalNetworkAddress(hostname: String): Boolean {
    return when {
        hostname == "localhost" -> true
        hostname == "127.0.0.1" -> true
        hostname.startsWith("192.168.") -> true
        hostname.startsWith("10.") -> true
        hostname.startsWith("172.16.") -> true
        hostname.startsWith("172.17.") -> true
        hostname.startsWith("172.18.") -> true
        hostname.startsWith("172.19.") -> true
        hostname.startsWith("172.20.") -> true
        hostname.startsWith("172.21.") -> true
        hostname.startsWith("172.22.") -> true
        hostname.startsWith("172.23.") -> true
        hostname.startsWith("172.24.") -> true
        hostname.startsWith("172.25.") -> true
        hostname.startsWith("172.26.") -> true
        hostname.startsWith("172.27.") -> true
        hostname.startsWith("172.28.") -> true
        hostname.startsWith("172.29.") -> true
        hostname.startsWith("172.30.") -> true
        hostname.startsWith("172.31.") -> true
        else -> false
    }
}
