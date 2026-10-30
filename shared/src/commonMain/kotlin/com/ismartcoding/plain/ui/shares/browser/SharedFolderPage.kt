package com.ismartcoding.plain.ui.shares.browser

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
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.downloads.components.DownloadListSheet
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.media.MediaPreviewData
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.navigation.LocalContentSource
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.rememberLifecycleEvent
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun SharedFolderPage(
    navController: NavHostController,
    messageId: String,
) {
    val source = LocalContentSource.current
    val scope = rememberCoroutineScope()
    val previewerState = rememberPreviewerState(scope = scope, pageCount = { MediaPreviewData.items.size })
    val state = remember(source, messageId) { SharedFolderState(messageId, navController, scope, previewerState, source) }
    val tasksMap = state.downloads.progress.collectAsState().value
    val shareTasks = tasksMap.values
        .filter { it.messageId == messageId }
    var showDownloads by remember { mutableStateOf(false) }

    LaunchedEffect(state) { state.loadMessage() }
    LaunchedEffect(state.shareMsg, state.currentPath) { state.ensureLoaded() }
    val lifecycle = rememberLifecycleEvent()
    LaunchedEffect(state, lifecycle) {
        if (lifecycle != Lifecycle.Event.ON_RESUME) return@LaunchedEffect
        try {
            state.downloads.refresh()
            if (source is ContentSource.Remote) {
                while (isActive) {
                    delay(1_000)
                    state.downloads.refresh()
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { DialogHelper.showMessage(error) }
    }
    LaunchedEffect(state) {
        state.downloads.errors.filterNotNull().collect { DialogHelper.showMessage(it) }
    }
    DisposableEffect(state) {
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
        DownloadListSheet(onDismiss = { showDownloads = false }, downloads = state.downloads)
    }

    MediaPreviewer(state = previewerState)
}
