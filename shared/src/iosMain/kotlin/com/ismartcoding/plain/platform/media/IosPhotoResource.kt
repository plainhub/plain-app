@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform.media

import com.ismartcoding.plain.platform.files.readResourceChunks
import kotlinx.cinterop.CPointer
import platform.Photos.*
import platform.posix.FILE
import platform.posix.fwrite
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData

internal object IosPhotoResource {
    fun resource(uri: String): PHAssetResource {
        require(uri.startsWith("ph://") && uri.length > 5) { "Photo resource URI required" }
        val fetched = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(uri.removePrefix("ph://")), null)
        check(fetched.count == 1UL) { "Photo resource unavailable" }
        val asset = fetched.objectAtIndex(0UL) as PHAsset
        return resource(asset)
    }

    fun resource(asset: PHAsset): PHAssetResource {
        val resources = PHAssetResource.assetResourcesForAsset(asset).map { it as PHAssetResource }
        return resources[resourceIndex(asset.mediaType, resources.map { it.type })]
    }

    internal fun resourceIndex(mediaType: PHAssetMediaType, types: List<PHAssetResourceType>): Int {
        val preferred = when (mediaType) {
            PHAssetMediaTypeImage -> listOf(PHAssetResourceTypeFullSizePhoto, PHAssetResourceTypePhoto)
            PHAssetMediaTypeVideo -> listOf(PHAssetResourceTypeFullSizeVideo, PHAssetResourceTypeVideo)
            else -> error("Unsupported photo resource type")
        }
        return preferred.firstNotNullOfOrNull { type -> types.indexOf(type).takeIf { it >= 0 } }
            ?: error("Photo resource unavailable")
    }

    suspend fun write(uri: String, file: CPointer<FILE>): Long {
        val resource = resource(uri)
        val manager = PHAssetResourceManager.defaultManager()
        val options = PHAssetResourceRequestOptions().apply { networkAccessAllowed = false }
        return readResourceChunks<NSData?>(
            start = { data, complete ->
                manager.requestDataForAssetResource(resource, options, data) { error ->
                    complete(error?.let { IllegalStateException(it.localizedDescription) })
                }
            },
            cancel = manager::cancelDataRequest,
            size = { data ->
                val chunk = checkNotNull(data) { "Photo resource data unavailable" }
                check(chunk.length <= Long.MAX_VALUE.toULong()) { "Photo resource length overflow" }
                chunk.length.toLong()
            },
            write = { data ->
                val chunk = checkNotNull(data) { "Photo resource data unavailable" }
                check(fwrite(chunk.bytes, 1UL, chunk.length, file) == chunk.length) { "Unable to read photo resource" }
            },
        )
    }
}
