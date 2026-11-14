@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform.media

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.Dimension
import coil3.size.Scale
import com.ismartcoding.plain.lib.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Photos.PHAsset
import platform.Photos.PHImageCancelledKey
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageContentModeAspectFit
import platform.Photos.PHImageContentModeDefault
import platform.Photos.PHImageErrorKey
import platform.Photos.PHImageManager
import platform.Photos.PHImageManagerMaximumSize
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeExact
import platform.Photos.PHImageResultIsDegradedKey
import platform.UIKit.UIImagePNGRepresentation

internal class IosPhotoFetcher(
    private val uri: Uri,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): ImageFetchResult {
        val reference = uri.toString()
        require(reference.startsWith("ph://") && reference.length > 5) { "Photo resource URI required" }
        val assets = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(reference.removePrefix("ph://")), null)
        check(assets.count == 1UL) { "Photo resource unavailable" }
        val asset = assets.objectAtIndex(0UL) as PHAsset
        val pixelWidth = asset.pixelWidth.toDouble()
        val pixelHeight = asset.pixelHeight.toDouble()
        check(pixelWidth > 0 && pixelHeight > 0) { "Photo resource has no image dimensions" }
        val width = (options.size.width as? Dimension.Pixels)?.px?.toDouble()
        val height = (options.size.height as? Dimension.Pixels)?.px?.toDouble()
        val targetSize = if (width == null && height == null) PHImageManagerMaximumSize.readValue() else CGSizeMake(
            width ?: checkNotNull(height) * pixelWidth / pixelHeight,
            height ?: checkNotNull(width) * pixelHeight / pixelWidth,
        )
        val contentMode = when {
            width == null && height == null -> PHImageContentModeDefault
            options.scale == Scale.FILL -> PHImageContentModeAspectFill
            else -> PHImageContentModeAspectFit
        }
        val manager = PHImageManager.defaultManager()
        val requestOptions = PHImageRequestOptions().apply {
            networkAccessAllowed = false
            synchronous = false
            deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
            resizeMode = PHImageRequestOptionsResizeModeExact
        }
        val bytes = suspendCancellableCoroutine<ByteArray> { continuation ->
            val requestId = manager.requestImageForAsset(
                asset, targetSize, contentMode, requestOptions,
            ) resultHandler@{ image, info ->
                if (!continuation.isActive) return@resultHandler
                val error = info?.get(PHImageErrorKey) as? NSError
                val canceled = (info?.get(PHImageCancelledKey) as? NSNumber)?.boolValue == true
                val degraded = (info?.get(PHImageResultIsDegradedKey) as? NSNumber)?.boolValue == true
                if (error == null && !canceled && degraded) return@resultHandler
                continuation.resumeWith(runCatching {
                    if (canceled) throw CancellationException("Photo thumbnail request canceled")
                    check(error == null) { "${error?.domain} (${error?.code}): ${error?.localizedDescription}" }
                    val thumbnail = checkNotNull(image) { "Photo thumbnail unavailable" }
                    checkNotNull(UIImagePNGRepresentation(thumbnail)) { "Could not encode photo thumbnail" }.toByteArray()
                })
            }
            continuation.invokeOnCancellation { manager.cancelImageRequest(requestId) }
        }
        val image = decodeIosImage(bytes)
        return ImageFetchResult(
            image = image,
            isSampled = image.width.toULong() < asset.pixelWidth || image.height.toULong() < asset.pixelHeight,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (data.scheme == "ph") IosPhotoFetcher(data, options) else null
    }
}
