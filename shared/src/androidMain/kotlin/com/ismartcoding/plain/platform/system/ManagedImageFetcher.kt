package com.ismartcoding.plain.platform.system

import coil3.ImageLoader
import coil3.fetch.Fetcher
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import coil3.toUri
import com.ismartcoding.plain.models.ManagedResource
import com.ismartcoding.plain.rust.graphql.createPeerResourceHttpClient

internal class ManagedImageFetcher : Fetcher.Factory<ManagedResource> {
    override fun create(data: ManagedResource, options: Options, imageLoader: ImageLoader): Fetcher? =
        OkHttpNetworkFetcherFactory(createPeerResourceHttpClient(data)).create(data.url.toUri(), options, imageLoader)
}
