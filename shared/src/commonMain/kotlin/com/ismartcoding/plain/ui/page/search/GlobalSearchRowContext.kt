package com.ismartcoding.plain.ui.page.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.base.dragselect.rememberDragSelectState
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.models.AudioQueueViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.GlobalSearchHit
import com.ismartcoding.plain.ui.models.GlobalSearchSource
import com.ismartcoding.plain.ui.models.TagsViewModel

/** Per-list dependencies for rows that reuse the source page's own list item component. */
class GlobalSearchRowContext(
    val navController: NavHostController,
    val audioQueueVM: AudioQueueViewModel,
    val audioVM: AudioViewModel,
    val tagsVM: TagsViewModel,
    val castVM: CastViewModel,
    val docsVM: DocsViewModel,
    val feedEntriesVM: FeedEntriesViewModel,
    val dragSelectState: DragSelectState,
    val itemState: TransformItemState,
    val previewerState: MediaPreviewerState,
)

@Composable
fun rememberGlobalSearchRowContext(
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    castVM: CastViewModel,
    docsVM: DocsViewModel,
    feedEntriesVM: FeedEntriesViewModel,
): GlobalSearchRowContext {
    val dragSelectState = rememberDragSelectState()
    val itemState = rememberTransformItemState()
    val previewerState = rememberPreviewerState()
    return remember(
        navController, audioQueueVM, audioVM, tagsVM, castVM, docsVM, feedEntriesVM,
        dragSelectState, itemState, previewerState,
    ) {
        GlobalSearchRowContext(
            navController, audioQueueVM, audioVM, tagsVM, castVM, docsVM, feedEntriesVM,
            dragSelectState, itemState, previewerState,
        )
    }
}

/** Preview payload of an image/video hit, or null for other domains. */
fun GlobalSearchHit.previewItem(): PreviewItem? = when (val s = source) {
    is GlobalSearchSource.Image -> PreviewItem(s.image.id, s.image.path, s.image.size, mediaId = s.image.id, data = s.image)
    is GlobalSearchSource.Video -> PreviewItem(s.video.id, s.video.path, s.video.size, mediaId = s.video.id, data = s.video)
    else -> null
}
