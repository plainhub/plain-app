package com.ismartcoding.plain.platform.system

import coil3.ImageLoader
import coil3.fetch.Fetcher
import coil3.network.NetworkFetcher
import coil3.request.Options
import coil3.toUri
import com.ismartcoding.plain.models.ManagedResource

internal class ManagedImageFetcher : Fetcher.Factory<ManagedResource> {
    override fun create(data: ManagedResource, options: Options, imageLoader: ImageLoader): Fetcher? =
        NetworkFetcher.Factory(networkClient = { IosImageNetworkClient(data.tlsCertificateSha256) }).create(data.url.toUri(), options, imageLoader)
}
