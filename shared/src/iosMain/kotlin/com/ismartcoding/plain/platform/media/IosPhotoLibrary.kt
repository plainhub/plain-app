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
    private data class MediaSnapshot(val revision: String, val assets: PHFetchResult?)
    private var mediaSnapshot: MediaSnapshot? = null
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
            if (!enabled && observing) { PHPhotoLibrary.sharedPhotoLibrary().unregisterChangeObserver(observer); observing = false; assets = null; mediaSnapshot = null; epoch++ }
        } finally { lock.unlock() }
    }
    private fun requirePermission() { check(Permission.READ_MEDIA_IMAGES.isGranted()) { "Image catalog permission denied" } }
    override fun snapshot(): JsonElement {
        requirePermission()
        return snapshot(true)
    }
    fun mediaSnapshot(): JsonElement = snapshot(false)
    private fun snapshot(imagesOnly: Boolean): JsonElement {
        lock.lock()
        try {
            val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
            val allowed = status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited
            observe(allowed)
            val fetched = if (!allowed) null else if (imagesOnly) PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, null)
                else PHAsset.fetchAssetsWithOptions(null)
            val previous = if (imagesOnly) assets else mediaSnapshot?.assets
            val changedSelection = status == PHAuthorizationStatusLimited && previous != null && fetched != null &&
                (previous.count != fetched.count || (0 until fetched.count.toInt()).any { index ->
                    (previous.objectAtIndex(index.toULong()) as PHAsset).localIdentifier !=
                        (fetched.objectAtIndex(index.toULong()) as PHAsset).localIdentifier
                })
            if (authorization != status || changedSelection) epoch++
            authorization = status
            if (imagesOnly) assets = fetched else mediaSnapshot = MediaSnapshot(epoch.toString(), fetched)
            return JsonHelper.jsonEncodeToElement(ImageCatalogSnapshot(epoch.toString(), fetched?.count?.toInt() ?: 0))
        } finally { lock.unlock() }
    }
    override fun verify(revision: String) {
        requirePermission()
        verify(revision, true)
    }
    fun verifyMedia(revision: String) = verify(revision, false)
    private fun verify(revision: String, imagesOnly: Boolean) {
        lock.lock()
        try {
            check(revision == epoch.toString() && (if (imagesOnly) assets != null else mediaSnapshot?.revision == revision) &&
                authorization == PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)) {
                "Photo catalog changed during indexing"
            }
        } finally { lock.unlock() }
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
        verifyMedia(revision)
        lock.lock()
        try {
            val fetched = checkNotNull(mediaSnapshot?.assets)
            require(offset <= fetched.count.toInt())
            return JsonHelper.jsonEncodeToElement((offset until minOf(offset + limit, fetched.count.toInt())).map { index ->
                val asset = fetched.objectAtIndex(index.toULong()) as PHAsset
                val resource = IosPhotoResource.resource(asset)
                fun timestamp(date: NSDate?): String = kotlin.time.Instant.fromEpochMilliseconds(((date?.timeIntervalSince1970 ?: 0.0) * 1000).toLong()).toString()
                PhotoCatalogAsset(asset.localIdentifier, resource.originalFilename, asset.mediaType.toInt(), (asset.duration * 1000).toLong(), asset.pixelWidth.toInt(), asset.pixelHeight.toInt(), timestamp(asset.creationDate), timestamp(asset.modificationDate ?: asset.creationDate), asset.favorite)
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
internal data class PhotoCatalogAsset(val id: String, val name: String, val mediaType: Int, val durationMs: Long, val width: Int, val height: Int, val createdAt: String, val updatedAt: String, val favorite: Boolean)
