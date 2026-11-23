package com.ismartcoding.plain.platform.system

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import com.ismartcoding.plain.models.ManagedResource

internal typealias ThumbnailNetworkSchedule = suspend (suspend () -> FetchResult?) -> FetchResult?

internal class VersionedManagedThumbnail(
    val cacheKey: String,
    private val networkSchedule: ThumbnailNetworkSchedule? = null,
    private val resolve: suspend () -> ManagedResource,
) {
    suspend fun fetch(cache: DiskCache?, fromNetwork: suspend (ManagedResource) -> FetchResult?): FetchResult? {
        val cached = cache?.openSnapshot(cacheKey)
        if (cached != null) {
            return SourceFetchResult(
                source = ImageSource(cached.data, cache.fileSystem, cacheKey, closeable = cached),
                mimeType = null,
                dataSource = DataSource.DISK,
            )
        }
        val load: suspend () -> FetchResult? = { fromNetwork(resolve()) }
        return if (networkSchedule == null) load() else networkSchedule(load)
    }
}

internal val VersionedManagedThumbnailKeyer = Keyer<VersionedManagedThumbnail> { thumbnail, _ -> thumbnail.cacheKey }

internal class VersionedManagedThumbnailFetcher : Fetcher.Factory<VersionedManagedThumbnail> {
    override fun create(data: VersionedManagedThumbnail, options: Options, imageLoader: ImageLoader): Fetcher =
        Fetcher {
            data.fetch(imageLoader.diskCache.takeIf { options.diskCachePolicy.readEnabled }) { resource ->
                imageLoader.components.newFetcher(resource, options, imageLoader)?.first?.fetch()
            }
        }
}
