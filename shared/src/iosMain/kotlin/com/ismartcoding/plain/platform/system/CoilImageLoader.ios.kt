package com.ismartcoding.plain.platform.system

import coil3.svg.SvgDecoder
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.memory.MemoryCache
import coil3.network.NetworkFetcher
import com.ismartcoding.plain.platform.media.IosPhotoFetcher

internal actual fun platformImageLoaderBuilder(context: PlatformContext): ImageLoader.Builder =
    ImageLoader.Builder(context)
        .components {
            add(ManagedImageFetcher())
            add(IosPhotoFetcher.Factory())
            add(SvgDecoder.Factory(true))
            add(NetworkFetcher.Factory(networkClient = { IosImageNetworkClient() }))
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, percent = 0.25)
                .build()
        }
