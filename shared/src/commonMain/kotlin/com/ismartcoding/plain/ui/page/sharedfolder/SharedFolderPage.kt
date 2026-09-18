package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.MediaPreviewer
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.models.MediaPreviewData

/**
 * Native browser for a shared folder card message, styled after FilesPage.
 * State and transfer orchestration live in SharedFolderState.kt, data
 * resolution in SharedFolderLoader.kt, the download engine in
 * SharedFolderTransfer.kt, and the UI pieces in SharedFolderTopBar.kt /
 * SharedFolderContent.kt / SharedFolderSheets.kt /
 * SharedFolderComponents.kt — this file is page orchestration only.
 */
@Composable
fun SharedFolderPage(
    navController: NavHostController,
    messageId: String,
) {
    val scope = rememberCoroutineScope()
    val previewerState = rememberPreviewerState(scope = scope, pageCount = { MediaPreviewData.items.size })
    val state = remember(messageId) { SharedFolderState(messageId, scope, previewerState) }

    LaunchedEffect(messageId) { state.loadMessage() }
    LaunchedEffect(state.shareMsg, state.currentPath) { state.ensureLoaded() }
    DisposableEffect(Unit) {
        onDispose { state.clearPreviewHandles() }
    }

    PScaffold(
        topBar = {
            SharedFolderTopBar(state, onClose = { navController.popBackStack() })
        },
    ) { paddingValues ->
        SharedFolderContent(state, paddingValues)
    }

    state.downloadTarget?.let { target ->
        SharedFolderDownloadSheet(state, target)
    }
    if (state.showSaveSelectedSheet) {
        SharedFolderSaveSelectionSheet(state)
    }

    MediaPreviewer(state = previewerState)
}
