package com.ismartcoding.plain.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.ui.platform.system.isGestureInteractionMode
import com.ismartcoding.plain.ui.media.extensions.reset
import androidx.compose.material3.ExperimentalMaterial3Api
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.files.MediaFoldersViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.launchSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioPageEffects(
    audioState: AudioPageState,
    audioVM: AudioViewModel,
    audioQueueVM: AudioQueueViewModel,
    tagsVM: TagsViewModel,
    mediaFoldersVM: MediaFoldersViewModel,
) {
    val scope = rememberCoroutineScope()
    val reloadAfterGrant: () -> Unit = {
        scope.launchSafe {
            audioVM.sortBy.value = Prefs.audioSortBy.value
            audioVM.loadAsync(tagsVM)
            audioQueueVM.loadAsync()
            mediaFoldersVM.loadAsync()
        }
    }

    com.ismartcoding.plain.ui.media.MediaPermissionEffects(audioVM.hasPermission, null, reloadAfterGrant, audioVM.source)

    LaunchedEffect(audioState.dragSelectState.selectMode, !isGestureInteractionMode()) {
        if (audioState.dragSelectState.selectMode || !isGestureInteractionMode()) {
            audioState.scrollBehavior.reset()
        }
    }
}
