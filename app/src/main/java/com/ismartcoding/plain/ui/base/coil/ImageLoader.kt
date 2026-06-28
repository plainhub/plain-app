package com.ismartcoding.plain.ui.base.coil

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.video.VideoFrameDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.ismartcoding.plain.activityManager
import com.ismartcoding.plain.api.KtorClientFactory
import com.ismartcoding.plain.api.OkHttpClientFactory

fun newImageLoader(context: PlatformContext): ImageLoader {
    // Always use applicationContext to avoid leaking Activity instances through
    // the lazy diskCache / memoryCache initializer lambdas held by RealImageLoader.
    val appContext = context.applicationContext
    val memoryPercent = if (activityManager.isLowRamDevice) 0.25 else 0.75

    // Dedicated OkHttp client for image loading. It uses the system trust store and
    // OkHttp's default OkHostnameVerifier, which is what every other Android image
    // fetcher does. We deliberately do NOT reuse `createUnsafeOkHttpClient()` here:
    // that client is built for the in-app HTTP server and only accepts
    // `isLocalNetworkAddress` hostnames, which silently broke every public https
    // image (the request fails with `SSLPeerUnverifiedException` before the
    // network is even reached).
    val imageLoaderClient = OkHttpClientFactory.createImageLoaderClient()

    return ImageLoader.Builder(appContext)
        .components {
            add(SvgDecoder.Factory(true))
            add(AnimatedImageDecoder.Factory())
            // ThumbnailDecoder must be before VideoFrameDecoder: for content:// video URIs,
            // ThumbnailDecoder uses ContentResolver.loadThumbnail() which reads the pre-generated
            // MediaStore thumbnail cache (fast). VideoFrameDecoder uses MediaMetadataRetriever
            // which opens and decodes the full video file (slow). ThumbnailDecoder only fires for
            // content:// URIs (ContentMetadata check), so file-based paths still reach VideoFrameDecoder.
            add(ThumbnailDecoder.Factory())
            add(VideoFrameDecoder.Factory()) // fallback for file:// video paths without content URI
            add(OkHttpNetworkFetcherFactory(imageLoaderClient))
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(appContext, percent = memoryPercent)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(appContext.cacheDir.resolve("image_cache").absoluteFile)
                .maxSizePercent(1.0)
                .build()
        }
        .crossfade(100)
        .build()
}
