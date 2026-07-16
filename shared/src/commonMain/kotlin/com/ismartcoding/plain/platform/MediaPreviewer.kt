package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel

/**
 * Full-screen media previewer overlay. Platform-specific (Android uses
 * `LocalContext`, Media3 video, etc.). iOS actual is a no-op.
 */
@Composable
expect fun MediaPreviewer(
    state: MediaPreviewerState,
    castVM: CastViewModel = viewModel(),
    tagsVM: TagsViewModel? = null,
    tagsMap: Map<String, List<DTagRelation>>? = null,
    tagsState: List<DTag> = emptyList(),
    onRenamed: () -> Unit = {},
    deleteAction: (PreviewItem) -> Unit = {},
    onTagsChanged: () -> Unit = {},
)
