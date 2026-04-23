package com.ismartcoding.plain.ui.page.files

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.appDir
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.events.ActionEvent
import com.ismartcoding.plain.events.FolderKanbanSelectEvent
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
internal fun FilesPageEffects(
    filesVM: FilesViewModel, context: Context, scope: CoroutineScope,
    folderPath: String, previewerState: MediaPreviewerState,
    audioPlaylistVM: AudioPlaylistViewModel,
) {
    BackHandler(enabled = previewerState.visible || filesVM.selectMode.value || filesVM.showSearchBar.value || filesVM.showPasteBar.value || filesVM.canNavigateBack()) {
        when {
            previewerState.visible -> scope.launch { previewerState.closeTransform() }
            filesVM.selectMode.value -> filesVM.exitSelectMode()
            filesVM.showSearchBar.value -> {
                filesVM.exitSearchMode()
                scope.launch(Dispatchers.IO) { filesVM.loadAsync(context) }
            }
            filesVM.showPasteBar.value -> { filesVM.cutFiles.clear(); filesVM.copyFiles.clear(); filesVM.showPasteBar.value = false }
            filesVM.canNavigateBack() -> {
                filesVM.navigateBack()
                scope.launch(Dispatchers.IO) { filesVM.loadAsync(context) }
            }
        }
    }

    LaunchedEffect(folderPath) {
        scope.launch(Dispatchers.IO) {
            if (folderPath.isNotEmpty()) {
                val targetDir = File(folderPath)
                if (targetDir.exists()) {
                    val appDataPath = context.appDir()
                    val type = if (folderPath.startsWith(appDataPath)) FilesType.APP else FilesType.INTERNAL_STORAGE
                    val rootPath = when (type) { FilesType.APP -> appDataPath; else -> FileSystemHelper.getInternalStoragePath() }
                    filesVM.initSelectedPath(rootPath, type, folderPath, folderPath)
                } else {
                    filesVM.loadLastPathAsync(context)
                }
            } else {
                filesVM.loadLastPathAsync(context)
            }
            filesVM.loadAsync(context)
            audioPlaylistVM.loadAsync(context)
        }
    }

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> scope.launch(Dispatchers.IO) { filesVM.loadAsync(context) }
                is FolderKanbanSelectEvent -> {
                    val m = event.data
                    filesVM.offset = 0
                    filesVM.initSelectedPath(m.rootPath, m.type, m.fullPath, m.fullPath)
                    scope.launch(Dispatchers.IO) { filesVM.loadAsync(context) }
                }
                is ActionEvent -> if (event.source == ActionSourceType.FILE) scope.launch(Dispatchers.IO) { filesVM.loadAsync(context) }
            }
        }
    }
}
