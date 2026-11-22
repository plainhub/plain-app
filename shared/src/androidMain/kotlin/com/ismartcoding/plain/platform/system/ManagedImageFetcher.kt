package com.ismartcoding.plain.platform.system

import coil3.ImageLoader
import coil3.fetch.Fetcher
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import coil3.toUri
import com.ismartcoding.plain.models.ManagedResource
import com.ismartcoding.plain.rust.graphql.peerImageHttpClient

internal class ManagedImageFetcher : Fetcher.Factory<ManagedResource> {
    override fun create(data: ManagedResource, options: Options, imageLoader: ImageLoader): Fetcher? =
        OkHttpNetworkFetcherFactory(
            callFactory = { peerImageHttpClient(data) },
            cacheStrategy = { ManagedImageCacheStrategy },
        ).create(data.url.toUri(), data.imageFetchOptions(options), imageLoader)
}
