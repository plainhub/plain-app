package com.ismartcoding.plain.ui.files

import com.ismartcoding.plain.rust.graphql.ContentSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.platform.system.appDir
import com.ismartcoding.plain.platform.files.fileExists
import com.ismartcoding.plain.platform.files.getInternalStoragePath
import com.ismartcoding.plain.features.files.FilesType
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.components.StoragePermissionResumeEffect
import com.ismartcoding.plain.ui.components.refreshStoragePermission
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.state.exitSearchMode
import com.ismartcoding.plain.ui.state.exitSelectMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.withContext

@Composable
internal fun FilesPageEffects(
    filesVM: FilesViewModel, scope: CoroutineScope,
    folderPath: String, previewerState: MediaPreviewerState,
    audioQueueVM: AudioQueueViewModel,
) {
    val reloadAfterGrant: () -> Unit = {
        scope.launchSafe { filesVM.loadAsync() }
    }
    if (filesVM.source is ContentSource.Local) StoragePermissionResumeEffect(filesVM.hasPermission, reloadAfterGrant)
    else com.ismartcoding.plain.ui.permissions.RemotePermissionEffects(
        filesVM.source as ContentSource.Remote, com.ismartcoding.plain.platform.permissions.Permission.WRITE_EXTERNAL_STORAGE, filesVM.hasPermission,
    )

    PBackHandler(enabled = previewerState.visible || filesVM.selectMode.value || filesVM.showSearchBar.value || filesVM.showPasteBar.value || filesVM.canNavigateBack()) {
        when {
            previewerState.visible -> scope.launch { previewerState.closeTransform() }
            filesVM.selectMode.value -> filesVM.exitSelectMode()
            filesVM.showSearchBar.value -> {
                filesVM.exitSearchMode()
                scope.launchSafe { filesVM.loadAsync() }
            }
            filesVM.showPasteBar.value -> { filesVM.cutFiles.clear(); filesVM.copyFiles.clear(); filesVM.showPasteBar.value = false }
            filesVM.canNavigateBack() -> {
                filesVM.navigateBack()
                scope.launchSafe { filesVM.loadAsync() }
            }
        }
    }

    LaunchedEffect(folderPath, if (filesVM.source is ContentSource.Remote) filesVM.hasPermission.value else true) {
        if (filesVM.source is ContentSource.Remote && !filesVM.hasPermission.value) return@LaunchedEffect
        try {
            withContext(Dispatchers.Default) {
                if (folderPath.isNotEmpty()) {
                    if (filesVM.source.exists(folderPath)) {
                        val appDataPath = filesVM.source.appPath
                        val type = if (folderPath.startsWith(appDataPath)) FilesType.APP else FilesType.INTERNAL_STORAGE
                        val rootPath = when (type) { FilesType.APP -> appDataPath; else -> filesVM.source.rootPath }
                        filesVM.initSelectedPath(rootPath, type, folderPath, folderPath)
                    } else {
                        filesVM.loadLastPathAsync()
                    }
                } else {
                    filesVM.loadLastPathAsync()
                }
                audioQueueVM.loadAsync()
                Prefs.showHiddenFiles.collect {
                    if (filesVM.hasPermission.value || filesVM.type == FilesType.APP) {
                        try { filesVM.loadAsync() }
                        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (error: Exception) { com.ismartcoding.plain.ui.components.DialogHelper.showMessage(error) }
                    }
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            filesVM.isLoading.value = false
            com.ismartcoding.plain.ui.components.DialogHelper.showMessage(error)
        }
    }
    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> if (filesVM.source is ContentSource.Local) refreshStoragePermission(filesVM.hasPermission, reloadAfterGrant)

            }
        }
    }
}
