package com.ismartcoding.plain.ui.page.media

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
import com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.platform.playlistAudioFromPath
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel

// Entry route for home-screen shortcut launches. Audio only: it starts playback
// and hands over to the audio player page. Images/videos are previewed as an
// overlay above the app UI instead (ShortcutMediaPreviewer), so they never land here.
@Composable
fun PlayMediaPage(
    navController: NavHostController,
    path: String,
    audioPlaylistVM: AudioPlaylistViewModel,
) {
    if (path.isAudioFast()) {
        LaunchedEffect(path) {
            coMain {
                val audio = withIO { playlistAudioFromPath(path) }
                withIO {
                    audioPlaylistVM.playlistItems.value = listOf(audio)
                    audioPlaylistVM.selectedPath.value = path
                }
                audioJustPlayWithNotificationCheck(audio)
                navController.navigate(Routing.Audio) {
                    popUpTo(Routing.PlayMedia(path)) { inclusive = true }
                }
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}