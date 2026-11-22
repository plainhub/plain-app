package com.ismartcoding.plain.platform.system

import coil3.annotation.ExperimentalCoilApi
import coil3.key.Keyer
import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.CachePolicy
import coil3.request.Options
import com.ismartcoding.plain.models.ManagedResource
import com.ismartcoding.plain.models.ManagedThumbnail

internal fun ManagedResource.imageCacheKey(): String? =
    if (thumbnail?.versioned == true) thumbnailDiskCacheKey() else null

internal fun ManagedResource.thumbnailDiskCacheKey(): String? =
    thumbnail?.let { thumbnailDiskCacheKey(tlsCertificateSha256, it) }

internal fun thumbnailDiskCacheKey(fingerprint: String, thumbnail: ManagedThumbnail): String =
    "$fingerprint:${thumbnail.cacheId}"

internal val ManagedImageCacheKeyer = Keyer<ManagedResource> { resource, _ -> resource.imageCacheKey() }

internal fun ManagedResource.imageFetchOptions(options: Options): Options {
    val key = thumbnailDiskCacheKey()
    return if (key == null) options.copy(diskCachePolicy = CachePolicy.DISABLED) else options.copy(diskCacheKey = key)
}

@OptIn(ExperimentalCoilApi::class)
internal class ManagedImageCacheStrategy private constructor(private val versioned: Boolean) : CacheStrategy {
    override suspend fun read(cacheResponse: NetworkResponse, networkRequest: NetworkRequest, options: Options): CacheStrategy.ReadResult {
        if (cacheResponse.code != 200) return CacheStrategy.ReadResult(networkRequest)
        if (versioned) return CacheStrategy.ReadResult(cacheResponse)
        val etag = cacheResponse.headers["ETag"] ?: return CacheStrategy.ReadResult(networkRequest)
        val headers = networkRequest.headers.newBuilder().apply { this["If-None-Match"] = etag }.build()
        return CacheStrategy.ReadResult(networkRequest.copy(headers = headers))
    }

    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ): CacheStrategy.WriteResult {
        if (networkResponse.code == 200 && (versioned || networkResponse.headers["ETag"] != null)) {
            return CacheStrategy.WriteResult(networkResponse)
        }
        if (!versioned && networkResponse.code == 304 && cacheResponse?.code == 200) {
            val etag = cacheResponse.headers["ETag"]
            if (etag != null && etag == networkRequest.headers["If-None-Match"]) {
                return CacheStrategy.WriteResult(cacheResponse.copy(body = null))
            }
        }
        return CacheStrategy.WriteResult.DISABLED
    }

    companion object {
        val Versioned = ManagedImageCacheStrategy(versioned = true)
        val Revalidating = ManagedImageCacheStrategy(versioned = false)
    }
}

internal fun ManagedResource.imageCacheStrategy(): ManagedImageCacheStrategy =
    if (thumbnail?.versioned == true) ManagedImageCacheStrategy.Versioned else ManagedImageCacheStrategy.Revalidating
