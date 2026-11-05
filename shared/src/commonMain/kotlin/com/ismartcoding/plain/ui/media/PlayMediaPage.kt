package com.ismartcoding.plain.ui.media

import com.ismartcoding.plain.features.media.MediaClient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.media.audioJustPlayWithPermissionCheck
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel

// Entry route for home-screen shortcut launches. Audio only: it starts playback
// and hands over to the audio player page. Images/videos are previewed as an
// overlay above the app UI instead (ShortcutMediaPreviewer), so they never land here.
@Composable
fun PlayMediaPage(
    navController: NavHostController,
    path: String,
    audioQueueVM: AudioQueueViewModel,
) {
    if (path.isAudioFast()) {
        LaunchedEffect(path) {
            coMain {
                val audio = withIO { MediaClient.audioInfo(path) }
                withIO { audioQueueVM.playSingleAsync(audio) }
                audioJustPlayWithPermissionCheck(audio)
                navController.navigate(Routing.Audio) {
                    popUpTo(Routing.PlayMedia(path)) { inclusive = true }
                }
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}
