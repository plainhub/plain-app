package com.ismartcoding.plain.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.audio.AudioViewModel
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.base.dragselect.rememberDragSelectState
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.feeds.FeedEntriesViewModel
import com.ismartcoding.plain.ui.files.documents.DocsViewModel
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.tags.TagsViewModel

class GlobalSearchRowContext internal constructor(
    val navController: NavHostController,
    source: ContentSource,
    owner: ViewModelStoreOwner,
    private val localAudioQueue: AudioQueueViewModel?,
    val dragSelectState: DragSelectState,
    val itemState: TransformItemState,
    val previewerState: MediaPreviewerState,
) {
    private val provider = ViewModelProvider.create(owner, viewModelFactory {
        initializer { AudioQueueViewModel(source) }
        initializer { AudioViewModel(source) }
        initializer { TagsViewModel(source.tags) }
        initializer { CastViewModel(source) }
        initializer { DocsViewModel(source) }
        initializer { FeedEntriesViewModel(source) }
    })
    val audioQueueVM by lazy { localAudioQueue ?: provider["searchAudioQueueVM", AudioQueueViewModel::class] }
    val audioVM by lazy { provider["searchAudioVM", AudioViewModel::class] }
    val tagsVM by lazy { provider["searchTagsVM", TagsViewModel::class] }
    val castVM by lazy { provider["searchCastVM", CastViewModel::class] }
    val docsVM by lazy { provider["searchDocsVM", DocsViewModel::class] }
    val feedEntriesVM by lazy { provider["searchFeedEntriesVM", FeedEntriesViewModel::class] }
}

@Composable
fun rememberGlobalSearchRowContext(navController: NavHostController, audioQueueVM: AudioQueueViewModel): GlobalSearchRowContext {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    val drag = rememberDragSelectState()
    val item = rememberTransformItemState()
    val preview = rememberPreviewerState()
    return remember(navController, audioQueueVM, owner) { GlobalSearchRowContext(navController, ContentSource.Local, owner, audioQueueVM, drag, item, preview) }
}

@Composable
internal fun rememberGlobalSearchDeviceContexts(local: GlobalSearchRowContext, devices: List<ContentSource>): Map<ContentSource, GlobalSearchRowContext> {
    val result = mutableMapOf<ContentSource, GlobalSearchRowContext>(ContentSource.Local to local)
    devices.filterIsInstance<ContentSource.Remote>().forEach { device ->
        key(device) {
            val owner = remember(device) { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
            DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
            val drag = rememberDragSelectState()
            val item = rememberTransformItemState()
            result[device] = remember(device, owner) { GlobalSearchRowContext(local.navController, device, owner, null, drag, item, local.previewerState) }
        }
    }
    return result
}

fun GlobalSearchHit.previewItem(): PreviewItem? = when (val value = source) {
    is GlobalSearchSource.Image -> PreviewItem(key, value.image.path, value.image.size, mediaId = value.image.id, data = value.image, source = device)
    is GlobalSearchSource.Video -> PreviewItem(key, value.video.path, value.video.size, mediaId = value.video.id, data = value.video, source = device)
    else -> null
}
