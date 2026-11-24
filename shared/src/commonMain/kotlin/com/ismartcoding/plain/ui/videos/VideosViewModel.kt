package com.ismartcoding.plain.ui.videos

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.media.BaseMediaViewModel

import com.ismartcoding.plain.models.Video as MediaVideo
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.models.MediaSortBy
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.tags.TagsViewModel
import kotlinx.coroutines.CoroutineScope

class VideosViewModel internal constructor(
    source: ContentSource = ContentSource.Local,
    private val gridCache: VideoGridCache? = null,
) : BaseMediaViewModel<MediaVideo>(
    source,
    DataType.VIDEO,
    pageSize = if (source is ContentSource.Remote) 256 else 1000,
    initialPageSize = if (source is ContentSource.Remote) 128 else 1000,
) {
    internal fun enterPage(scope: CoroutineScope, tags: TagsViewModel, loadFolders: suspend () -> Unit) =
        scope.launchSafe(onDone = { showLoading.value = false }) {
            showCachedGrid()
            loadAsync(tags)
            loadFolders()
        }

    internal suspend fun showCachedGrid() {
        if (source !is ContentSource.Remote || !hasPermission.value || itemsFlow.value.isNotEmpty() ||
            queryText.value.isNotBlank() || trash.value || tag.value != null || bucketId.value.isNotEmpty()) return
        val sort = sortBy.value
        val cached = withIO { gridCache?.read(source.client.videoCatalogCacheScope(), sort) } ?: return
        if (cached.isEmpty() || itemsFlow.value.isNotEmpty() || sortBy.value != sort || !hasPermission.value) return
        noMore.value = true
        _itemsFlow.value = cached
    }

    override fun onInitialRows(query: String, sort: MediaSortBy, items: List<MediaVideo>) {
        if (source is ContentSource.Remote && query.trim() == "trash:false") {
            gridCache?.write(source.client.videoCatalogCacheScope(), sort, items)
        }
    }
}
