package com.ismartcoding.plain.ui.page.media

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.nav.Routing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayMediaPage(
    navController: NavHostController,
    path: String,
    audioPlaylistVM: AudioPlaylistViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (path.isAudioFast()) {
        LaunchedEffect(path) {
            Permissions.checkNotification(context, R.string.audio_notification_prompt) {
                coMain {
                    val audio = withIO { DPlaylistAudio.fromPath(context, path) }
                    withIO {
                        audioPlaylistVM.playlistItems.value = listOf(audio)
                        audioPlaylistVM.selectedPath.value = path
                    }
                    AudioPlayer.play(context, audio)
                    navController.navigate(Routing.Audio) {
                        popUpTo(Routing.PlayMedia(path)) { inclusive = true }
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    MediaPreviewData.items = listOf(PreviewItem(id = path, path = path))

    val previewerState = rememberPreviewerState(
        scope = scope,
        pageCount = { MediaPreviewData.items.size },
    )

    var hasOpened by remember { mutableStateOf(false) }

    LaunchedEffect(previewerState.visible) {
        if (previewerState.visible) {
            hasOpened = true
        } else if (hasOpened && !previewerState.animating) {
            navController.navigateUp()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        LaunchedEffect(Unit) {
            previewerState.open(0)
        }
        MediaPreviewer(state = previewerState)
    }
}
