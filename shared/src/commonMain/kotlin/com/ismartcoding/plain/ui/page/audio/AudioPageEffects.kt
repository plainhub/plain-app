package com.ismartcoding.plain.ui.page.audio
import com.ismartcoding.plain.preferences.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.lib.channel.Channel
import com.ismartcoding.plain.platform.isGestureInteractionMode
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.hasPermission
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.preferences.AudioSortByPreference
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.audio.AudioPageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioPageEffects(
    audioState: AudioPageState,
    audioVM: AudioViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    tagsVM: TagsViewModel,
    mediaFoldersVM: MediaFoldersViewModel,
) {
    val scope = rememberCoroutineScope()
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(Unit) {
        audioVM.hasPermission.value = AppFeatureType.FILES.hasPermission()
        if (audioVM.hasPermission.value) {
            scope.launch(Dispatchers.Default) {
                audioVM.sortBy.value = AudioSortByPreference.getValueAsync()
                audioVM.loadAsync(tagsVM)
                audioPlaylistVM.loadAsync()
                mediaFoldersVM.loadAsync()
            }
        }
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            if (event is PermissionsResultEvent) {
                audioVM.hasPermission.value = AppFeatureType.FILES.hasPermission()
                scope.launch(Dispatchers.Default) {
                    audioVM.sortBy.value = AudioSortByPreference.getValueAsync()
                    audioVM.loadAsync(tagsVM)
                }
            }
        }
    }

    LaunchedEffect(audioState.dragSelectState.selectMode, !isGestureInteractionMode()) {
        if (audioState.dragSelectState.selectMode || !isGestureInteractionMode()) {
            audioState.scrollBehavior.reset()
        }
    }
}
