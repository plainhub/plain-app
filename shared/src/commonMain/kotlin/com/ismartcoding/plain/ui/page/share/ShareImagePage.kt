package com.ismartcoding.plain.ui.page.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.platform.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformItemState

/**
 * In-app viewer for a shared image (arbitrary file path, e.g. materialized
 * from a share intent). Hosts the standard media previewer with a single
 * item and pops the route when the user closes it.
 */
@Composable
fun ShareImagePage(navController: NavHostController, path: String, name: String) {
    val previewerState = rememberPreviewerState()
    val itemState = rememberTransformItemState()
    var seenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(path) {
        val item = PreviewItem("share", path, 0L, data = DMessageFile(uri = path, size = 0L, fileName = name))
        MediaPreviewData.setDataAsync(itemState, listOf(item), item)
        previewerState.open()
    }

    LaunchedEffect(previewerState.visible) {
        if (previewerState.visible) {
            seenVisible = true
        } else if (seenVisible) {
            navController.popBackStack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MediaPreviewer(state = previewerState)
    }
}
