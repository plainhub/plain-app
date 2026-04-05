package com.ismartcoding.plain.ui.page.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.isGestureInteractionMode
import com.ismartcoding.plain.enums.AppFeatureType
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
    audioState: AudioPageState, audioVM: AudioViewModel,
    audioPlaylistVM: AudioPlaylistViewModel, tagsVM: TagsViewModel,
    mediaFoldersVM: MediaFoldersViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(Unit) {
        audioVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
        if (audioVM.hasPermission.value) {
            scope.launch(Dispatchers.IO) {
                audioVM.sortBy.value = AudioSortByPreference.getValueAsync(context)
                audioVM.loadAsync(context, tagsVM)
                audioPlaylistVM.loadAsync(context)
                mediaFoldersVM.loadAsync(context)
            }
        }
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            if (event is PermissionsResultEvent) {
                audioVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
                scope.launch(Dispatchers.IO) {
                    audioVM.sortBy.value = AudioSortByPreference.getValueAsync(context)
                    audioVM.loadAsync(context, tagsVM)
                }
            }
        }
    }

    LaunchedEffect(audioState.dragSelectState.selectMode, !context.isGestureInteractionMode()) {
        if (audioState.dragSelectState.selectMode || !context.isGestureInteractionMode()) {
            audioState.scrollBehavior.reset()
        }
    }
}
