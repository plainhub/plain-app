@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.ai.ImageCatalogItem
import com.ismartcoding.plain.ai.ImageCatalogPage
import com.ismartcoding.plain.ai.ImageCatalogSnapshot
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.cinterop.useContents
import kotlinx.serialization.json.*
import platform.Foundation.*
import platform.Photos.*
import platform.UIKit.*
import platform.CoreGraphics.CGRectMake
import platform.darwin.NSObject

internal object IosImageIndexCatalog : NSObject(), PHPhotoLibraryChangeObserverProtocol {
    private val lock = NSRecursiveLock()
    private var epoch = 0L
    private var observing = false
    private var assets: PHFetchResult? = null
    override fun photoLibraryDidChange(changeInstance: PHChange) {
        lock.lock()
        try { epoch++ } finally { lock.unlock() }
        com.ismartcoding.plain.lib.coIO {
            if (com.ismartcoding.plain.ai.RustImageModels.snapshot.value.status == com.ismartcoding.plain.ai.ImageSearchStatusType.READY) {
                com.ismartcoding.plain.features.imageindex.ImageIndexHelper.start(false)
            }
        }
    }
    fun observe(enabled: Boolean) {
        lock.lock()
        try {
            if (enabled && !observing) { PHPhotoLibrary.sharedPhotoLibrary().registerChangeObserver(this); observing = true }
            if (!enabled && observing) { PHPhotoLibrary.sharedPhotoLibrary().unregisterChangeObserver(this); observing = false; assets = null; epoch++ }
        } finally { lock.unlock() }
    }
    private fun requirePermission() { check(Permission.READ_MEDIA_IMAGES.isGranted()) { "Image catalog permission denied" } }
    fun snapshot(): JsonElement {
        requirePermission()
        observe(true)
        lock.lock()
        try {
            val fetched = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, null)
            assets = fetched
            return JsonHelper.jsonEncodeToElement(ImageCatalogSnapshot(epoch.toString(), fetched.count.toInt()))
        } finally { lock.unlock() }
    }
    fun verify(revision: String) {
        requirePermission()
        lock.lock()
        try { check(revision == epoch.toString() && assets != null) { "Image catalog changed during indexing" } }
        finally { lock.unlock() }
    }
    fun page(revision: String, cursor: String, limit: Int): JsonElement {
        require(limit in 1..128)
        verify(revision)
        lock.lock()
        try {
            val fetched = checkNotNull(assets)
            val start = if (cursor.isEmpty()) 0 else cursor.toInt()
            require(start >= 0 && start <= fetched.count.toInt())
            val end = minOf(start + limit, fetched.count.toInt())
            val items = (start until end).map { index ->
                val asset = fetched.objectAtIndex(index.toULong()) as PHAsset
                ImageCatalogItem(asset.localIdentifier, "ph://${asset.localIdentifier}")
            }
            return JsonHelper.jsonEncodeToElement(ImageCatalogPage(revision, items, end.toString(), end == fetched.count.toInt()))
        } finally { lock.unlock() }
    }
    fun resolve(revision: String, ids: List<String>): JsonElement {
        require(ids.size <= 100)
        verify(revision)
        val fetched = PHAsset.fetchAssetsWithLocalIdentifiers(ids, null)
        val items = (0 until fetched.count.toInt()).map { index ->
            val asset = fetched.objectAtIndex(index.toULong()) as PHAsset
            ImageCatalogItem(asset.localIdentifier, "ph://${asset.localIdentifier}")
        }
        verify(revision)
        return JsonHelper.jsonEncodeToElement(items)
    }
    fun decode(id: String): String? {
        requirePermission()
        val fetched = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(id), null)
        if (fetched.count == 0UL) return null
        val asset = fetched.objectAtIndex(0UL) as PHAsset
        val options = PHImageRequestOptions().apply { synchronous = true; networkAccessAllowed = false }
        var path: String? = null
        PHImageManager.defaultManager().requestImageDataAndOrientationForAsset(asset, options) { data, _, _, _ ->
            if (data != null) {
                val image = UIImage(data = data) ?: return@requestImageDataAndOrientationForAsset
                val (width, height) = image.size.useContents { width to height }
                if (width >= 64 && height >= 64 && width * height <= 32 * 1024 * 1024) {
                    UIGraphicsBeginImageContextWithOptions(image.size, false, 1.0)
                    try {
                        image.drawInRect(CGRectMake(0.0, 0.0, width, height))
                        val normalized = UIGraphicsGetImageFromCurrentImageContext()
                        val png = normalized?.let { UIImagePNGRepresentation(it) }
                        val file = NSTemporaryDirectory() + "image-index-${NSUUID().UUIDString}.png"
                        if (png?.writeToFile(file, true) == true) path = file
                    } finally { UIGraphicsEndImageContext() }
                }
            }
        }
        return path
    }
    fun release(path: String): Boolean {
        val root = NSURL.fileURLWithPath(NSTemporaryDirectory()).URLByStandardizingPath!!.path!!.trimEnd('/')
        val file = NSURL.fileURLWithPath(path).URLByStandardizingPath!!.path!!
        require(file.substringBeforeLast('/') == root && file.substringAfterLast('/').startsWith("image-index-"))
        return NSFileManager.defaultManager.removeItemAtPath(file, null)
    }
}
