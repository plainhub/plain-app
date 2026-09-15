package com.ismartcoding.plain.ui.page.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.platform.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.models.MediaPreviewData

// Fullscreen media preview hosted above the NavHost for home-screen shortcut
// launches. Unlike Routing.PlayMedia (a black immersive page), this keeps the
// actual app UI composed beneath, so closing the preview reveals it directly.
@Composable
fun ShortcutMediaPreviewer(path: String, onClosed: () -> Unit) {
    val scope = rememberCoroutineScope()

    MediaPreviewData.items = listOf(PreviewItem(id = path, path = path))

    val previewerState = rememberPreviewerState(
        scope = scope,
        pageCount = { MediaPreviewData.items.size },
    )

    var hasOpened by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        previewerState.open(0)
    }

    LaunchedEffect(previewerState.visible) {
        if (previewerState.visible) {
            hasOpened = true
        } else if (hasOpened && !previewerState.animating) {
            onClosed()
        }
    }

    MediaPreviewer(state = previewerState)
}