@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform.media

import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*
import platform.Foundation.*
import platform.Photos.*
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi

internal object IosPhotoLibrary : ImageCatalog {
    private val lock = NSRecursiveLock()
    private var epoch = 0L
    private var observing = false
    private var assets: PHFetchResult? = null
    private var authorization = PHAuthorizationStatusNotDetermined
    private val observer = object : NSObject(), PHPhotoLibraryChangeObserverProtocol {
        override fun photoLibraryDidChange(changeInstance: PHChange) {
            lock.lock()
            try { epoch++ } finally { lock.unlock() }
            reportImageCatalogChanged()
        }
    }
    fun observe(enabled: Boolean) {
        lock.lock()
        try {
            if (enabled && !observing) { PHPhotoLibrary.sharedPhotoLibrary().registerChangeObserver(observer); observing = true }
            if (!enabled && observing) { PHPhotoLibrary.sharedPhotoLibrary().unregisterChangeObserver(observer); observing = false; assets = null; epoch++ }
        } finally { lock.unlock() }
    }
    private fun requirePermission() { check(Permission.READ_MEDIA_IMAGES.isGranted()) { "Image catalog permission denied" } }
    override fun snapshot(): JsonElement {
        requirePermission()
        observe(true)
        lock.lock()
        try {
            val fetched = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, null)
            val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
            val previous = assets
            val changedSelection = status == PHAuthorizationStatusLimited && (previous == null || previous.count != fetched.count || (0 until fetched.count.toInt()).any { index ->
                (previous.objectAtIndex(index.toULong()) as PHAsset).localIdentifier != (fetched.objectAtIndex(index.toULong()) as PHAsset).localIdentifier
            })
            if (authorization != status || changedSelection) epoch++
            authorization = status
            assets = fetched
            return JsonHelper.jsonEncodeToElement(ImageCatalogSnapshot(epoch.toString(), fetched.count.toInt()))
        } finally { lock.unlock() }
    }
    override fun verify(revision: String) {
        requirePermission()
        lock.lock()
        try { check(revision == epoch.toString() && assets != null) { "Image catalog changed during indexing" } }
        finally { lock.unlock() }
    }
    override fun page(revision: String, cursor: String, limit: Int): JsonElement {
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
    fun mediaPage(revision: String, offset: Int, limit: Int): JsonElement {
        require(limit in 1..128 && offset >= 0)
        verify(revision)
        lock.lock()
        try {
            val fetched = checkNotNull(assets)
            require(offset <= fetched.count.toInt())
            return JsonHelper.jsonEncodeToElement((offset until minOf(offset + limit, fetched.count.toInt())).map { index ->
                val asset = fetched.objectAtIndex(index.toULong()) as PHAsset
                val resource = IosPhotoResource.resource(asset)
                fun timestamp(date: NSDate?): String = kotlin.time.Instant.fromEpochMilliseconds(((date?.timeIntervalSince1970 ?: 0.0) * 1000).toLong()).toString()
                PhotoCatalogAsset(asset.localIdentifier, resource.originalFilename, asset.pixelWidth.toInt(), asset.pixelHeight.toInt(), timestamp(asset.creationDate), timestamp(asset.modificationDate ?: asset.creationDate), asset.favorite)
            })
        } finally { lock.unlock() }
    }
    override fun resolve(revision: String, ids: List<String>): JsonElement {
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
}

@kotlinx.serialization.Serializable
internal data class PhotoCatalogAsset(val id: String, val name: String, val width: Int, val height: Int, val createdAt: String, val updatedAt: String, val favorite: Boolean)
