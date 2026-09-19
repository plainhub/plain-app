package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.download.DownloadCenter
import com.ismartcoding.plain.features.share.SharedFolderBatchTask
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.MediaPreviewer
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.components.downloads.DownloadListSheet
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.models.MediaPreviewData
import org.jetbrains.compose.resources.stringResource

/**
 * Native browser for a shared folder card message, styled after FilesPage.
 * Browsing/preview state lives in SharedFolderState.kt, data resolution in
 * SharedFolderLoader.kt, transfer primitives in SharedFolderTransfer.kt and
 * batch downloads in features/share/SharedFolderDownloadEngine.kt — this
 * file is page orchestration only. Downloads keep running after the page is
 * closed; the app-level floating widget (MainNavGraph) exposes them.
 */
@Composable
fun SharedFolderPage(
    navController: NavHostController,
    messageId: String,
) {
    val scope = rememberCoroutineScope()
    val previewerState = rememberPreviewerState(scope = scope, pageCount = { MediaPreviewData.items.size })
    val state = remember(messageId) { SharedFolderState(messageId, scope, previewerState) }
    val tasksMap = DownloadCenter.progress.collectAsState().value
    val shareTasks = tasksMap.values
        .filterIsInstance<SharedFolderBatchTask>()
        .filter { it.messageId == messageId }
    var showDownloads by remember { mutableStateOf(false) }

    LaunchedEffect(messageId) { state.loadMessage() }
    LaunchedEffect(state.shareMsg, state.currentPath) { state.ensureLoaded() }
    DisposableEffect(Unit) {
        onDispose { state.clearPreviewHandles() }
    }

    PScaffold(
        topBar = {
            SharedFolderTopBar(state, onClose = { navController.popBackStack() })
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.selectMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                PBottomAppBar {
                    BottomActionButtons {
                        PFilledButton(
                            text = stringResource(Res.string.save_selected, state.selected.size),
                            onClick = { state.showSaveSelectedSheet = true },
                            enabled = state.selected.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        SharedFolderContent(
            state = state,
            contentPadding = paddingValues,
            tasks = shareTasks,
            onOpenDownloads = { showDownloads = true },
        )
    }

    state.downloadTarget?.let { target ->
        SharedFolderDownloadSheet(state, target)
    }
    if (state.showSaveSelectedSheet) {
        SharedFolderSaveSelectionSheet(state)
    }
    if (showDownloads) {
        DownloadListSheet(onDismiss = { showDownloads = false })
    }

    MediaPreviewer(state = previewerState)
}
