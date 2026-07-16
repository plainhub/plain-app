package com.ismartcoding.plain.ui.page.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ismartcoding.plain.lib.channel.Channel
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.platform.appDir
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.getInternalStoragePath
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.events.ActionEvent
import com.ismartcoding.plain.events.FolderKanbanSelectEvent
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun FilesPageEffects(
    filesVM: FilesViewModel, scope: CoroutineScope,
    folderPath: String, previewerState: MediaPreviewerState,
    audioPlaylistVM: AudioPlaylistViewModel,
) {
    PBackHandler(enabled = previewerState.visible || filesVM.selectMode.value || filesVM.showSearchBar.value || filesVM.showPasteBar.value || filesVM.canNavigateBack()) {
        when {
            previewerState.visible -> scope.launch { previewerState.closeTransform() }
            filesVM.selectMode.value -> filesVM.exitSelectMode()
            filesVM.showSearchBar.value -> {
                filesVM.exitSearchMode()
                scope.launch(Dispatchers.Default) { filesVM.loadAsync() }
            }
            filesVM.showPasteBar.value -> { filesVM.cutFiles.clear(); filesVM.copyFiles.clear(); filesVM.showPasteBar.value = false }
            filesVM.canNavigateBack() -> {
                filesVM.navigateBack()
                scope.launch(Dispatchers.Default) { filesVM.loadAsync() }
            }
        }
    }

    LaunchedEffect(folderPath) {
        scope.launch(Dispatchers.Default) {
            if (folderPath.isNotEmpty()) {
                if (fileExists(folderPath)) {
                    val appDataPath = appDir()
                    val type = if (folderPath.startsWith(appDataPath)) FilesType.APP else FilesType.INTERNAL_STORAGE
                    val rootPath = when (type) { FilesType.APP -> appDataPath; else -> getInternalStoragePath() }
                    filesVM.initSelectedPath(rootPath, type, folderPath, folderPath)
                } else {
                    filesVM.loadLastPathAsync()
                }
            } else {
                filesVM.loadLastPathAsync()
            }
            filesVM.loadAsync()
            audioPlaylistVM.loadAsync()
        }
    }

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> scope.launch(Dispatchers.Default) { filesVM.loadAsync() }
                is FolderKanbanSelectEvent -> {
                    val m = event.data
                    filesVM.offset = 0
                    filesVM.initSelectedPath(m.rootPath, m.type, m.fullPath, m.fullPath)
                    scope.launch(Dispatchers.Default) { filesVM.loadAsync() }
                }
                is ActionEvent -> if (event.source == ActionSourceType.FILE) scope.launch(Dispatchers.Default) { filesVM.loadAsync() }
            }
        }
    }
}
