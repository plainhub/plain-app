package com.ismartcoding.plain.ui.base.coil

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.video.VideoFrameDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.util.DebugLogger
import com.ismartcoding.plain.activityManager
import com.ismartcoding.plain.api.HttpClientManager

fun newImageLoader(context: PlatformContext): ImageLoader {
    // Always use applicationContext to avoid leaking Activity instances through
    // the lazy diskCache / memoryCache initializer lambdas held by RealImageLoader.
    val appContext = context.applicationContext
    val memoryPercent = if (activityManager.isLowRamDevice) 0.25 else 0.75
    
    val unsafeOkHttpClient = HttpClientManager.createUnsafeOkHttpClient()
    
    return ImageLoader.Builder(appContext)
        .components {
            add(SvgDecoder.Factory(true))
            add(AnimatedImageDecoder.Factory())
            add(VideoFrameDecoder.Factory()) // enables thumbnail extraction for videos with known extensions
            add(ThumbnailDecoder.Factory())
            add(OkHttpNetworkFetcherFactory(unsafeOkHttpClient))
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
        .allowRgb565(true)
        .logger(DebugLogger())
        .build()
}
