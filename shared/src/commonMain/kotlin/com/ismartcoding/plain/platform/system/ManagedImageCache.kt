package com.ismartcoding.plain.platform.system

import coil3.annotation.ExperimentalCoilApi
import coil3.key.Keyer
import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.CachePolicy
import coil3.request.Options
import com.ismartcoding.plain.models.ManagedResource

internal fun ManagedResource.imageCacheKey(): String? =
    if (versionedThumbnail) "$tlsCertificateSha256:$url" else null

internal val ManagedImageCacheKeyer = Keyer<ManagedResource> { resource, _ -> resource.imageCacheKey() }

internal fun ManagedResource.imageFetchOptions(options: Options): Options {
    val key = imageCacheKey()
    return if (key == null) options.copy(diskCachePolicy = CachePolicy.DISABLED) else options.copy(diskCacheKey = key)
}

@OptIn(ExperimentalCoilApi::class)
internal object ManagedImageCacheStrategy : CacheStrategy {
    override suspend fun read(cacheResponse: NetworkResponse, networkRequest: NetworkRequest, options: Options) =
        if (cacheResponse.code == 200) CacheStrategy.ReadResult(cacheResponse) else CacheStrategy.ReadResult(networkRequest)

    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ) = if (networkResponse.code == 200) CacheStrategy.WriteResult(networkResponse) else CacheStrategy.WriteResult.DISABLED
}
