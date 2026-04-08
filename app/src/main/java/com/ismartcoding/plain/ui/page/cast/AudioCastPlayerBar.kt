package com.ismartcoding.plain.ui.page.cast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.launch

@Composable
fun AudioCastPlayerBar(
    castVM: CastViewModel,
    modifier: Modifier = Modifier,
    dragSelectState: DragSelectState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var showCastPlaylist by remember { mutableStateOf(false) }
    val isPlaying by CastPlayer.isPlaying.collectAsState()
    val progress by CastPlayer.progress.collectAsState()
    val duration by CastPlayer.duration.collectAsState()
    val supportsCallback by CastPlayer.supportsCallback.collectAsState()
    val currentUri by CastPlayer.currentUri.collectAsState()

    LaunchedEffect(currentUri) {
        scope.launch {
            if (currentUri.isNotEmpty()) {
                val audio = withIO { DPlaylistAudio.fromPath(context, currentUri) }
                title = audio.title
                artist = audio.artist
            } else {
                title = ""
                artist = ""
            }
        }
    }

    AnimatedVisibility(
        visible = castVM.castMode.value && CastPlayer.currentDevice != null && !dragSelectState.selectMode,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        ElevatedCard(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(PlainTheme.CARD_RADIUS),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AudioCastPlayerBarContent(
                castVM = castVM,
                title = title, artist = artist,
                isPlaying = isPlaying, progress = progress, duration = duration,
                supportsCallback = supportsCallback, currentUri = currentUri,
                onShowPlaylist = { showCastPlaylist = true },
            )
        }
    }

    if (showCastPlaylist) {
        AudioCastPlaylistPage(castVM = castVM, onDismissRequest = { showCastPlaylist = false })
    }
}
