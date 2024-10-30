package com.ismartcoding.plain.ui.page.audio

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.formatDuration
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.preference.AudioPlayModePreference
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.WaveSlider
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerPage(
    audioPlaylistVM: AudioPlaylistViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    val isPlaying by AudioPlayer.isPlayingFlow.collectAsState()
    var playMode by remember { mutableStateOf(TempData.audioPlayMode) }
    var showPlaylist by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var isTimerActive by remember { mutableStateOf(false) }
    val currentPlayingPath = audioPlaylistVM.selectedPath

    LaunchedEffect(currentPlayingPath.value) {
        scope.launch {
            val path = currentPlayingPath.value
            if (path.isNotEmpty()) {
                val audio = withIO { DPlaylistAudio.fromPath(context, path) }
                duration = audio.duration.toFloat()
                if (!isDragging) {
                    progress = AudioPlayer.playerProgress / 1000f
                }
                title = audio.title
                artist = audio.artist
            }
            playMode = TempData.audioPlayMode
            isTimerActive = TempData.audioSleepTimerFutureTime > SystemClock.elapsedRealtime()
        }
    }

    // Progress update - only run when playing and not dragging
    LaunchedEffect(isPlaying, isDragging) {
        if (isPlaying && !isDragging) {
            while (true) {
                progress = AudioPlayer.playerProgress / 1000f
                delay(1000)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            isTimerActive = TempData.audioSleepTimerFutureTime > SystemClock.elapsedRealtime()
            delay(1000)
        }
    }

    PModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Album Art - Larger and centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.music2),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
            }

            // Song Info - Simplified and elegant with scrolling text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title with horizontal scroll for long text
                val titleScrollState = rememberScrollState()
                val isScrollingTitle = remember { mutableStateOf(false) }

                LaunchedEffect(title) {
                    delay(1500) // Wait for layout
                    if (titleScrollState.maxValue > 0) {
                        isScrollingTitle.value = true
                        while (isScrollingTitle.value) {
                            titleScrollState.animateScrollTo(0)
                            delay(2000)
                            titleScrollState.animateScrollTo(titleScrollState.maxValue)
                            delay(2000)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier
                            .horizontalScroll(titleScrollState, reverseScrolling = false)
                    )
                }

                VerticalSpace(8.dp)

                val artistScrollState = rememberScrollState()
                val isScrollingArtist = remember { mutableStateOf(false) }

                LaunchedEffect(artist) {
                    delay(1500) // Wait for layout
                    if (artistScrollState.maxValue > 0) {
                        isScrollingArtist.value = true
                        while (isScrollingArtist.value) {
                            artistScrollState.animateScrollTo(0)
                            delay(2000)
                            artistScrollState.animateScrollTo(artistScrollState.maxValue)
                            delay(2000)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier
                            .horizontalScroll(artistScrollState, reverseScrolling = false)
                    )
                }
            }

            VerticalSpace(32.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                WaveSlider(
                    value = progress,
                    onValueChange = { newValue ->
                        isDragging = true
                        progress = minOf(newValue, duration)
                    },
                    onValueChangeFinished = {
                        if (duration > 0 && progress >= 0) {
                            AudioPlayer.seekTo(progress.toLong())
                        }
                        isDragging = false
                    },
                    valueRange = 0f..maxOf(duration, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    isPlaying = isPlaying
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = (AudioPlayer.playerProgress / 1000).formatDuration(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = duration.toLong().formatDuration(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play Mode
                IconButton(
                    onClick = {
                        scope.launch {
                            val nextMode = when (playMode) {
                                MediaPlayMode.REPEAT -> MediaPlayMode.REPEAT_ONE
                                MediaPlayMode.REPEAT_ONE -> MediaPlayMode.SHUFFLE
                                MediaPlayMode.SHUFFLE -> MediaPlayMode.REPEAT
                            }
                            TempData.audioPlayMode = nextMode
                            playMode = nextMode
                            withIO { AudioPlayModePreference.putAsync(context, nextMode) }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        painter = painterResource(
                            when (playMode) {
                                MediaPlayMode.REPEAT -> R.drawable.repeat
                                MediaPlayMode.REPEAT_ONE -> R.drawable.repeat1
                                MediaPlayMode.SHUFFLE -> R.drawable.shuffle
                            }
                        ),
                        contentDescription = "Play mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Sleep Timer
                IconButton(
                    onClick = { showSleepTimer = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTimerActive)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.timer),
                        contentDescription = "Sleep timer",
                        tint = if (isTimerActive)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Playlist
                IconButton(
                    onClick = { showPlaylist = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.list_music),
                        contentDescription = "Playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main playback controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                IconButton(
                    onClick = { AudioPlayer.skipToPrevious() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (isPlaying) {
                            AudioPlayer.pause()
                        } else {
                            AudioPlayer.play()
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play_arrow),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(
                    onClick = { AudioPlayer.skipToNext() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }

    if (showSleepTimer) {
        SleepTimerPage(
            onDismissRequest = {
                showSleepTimer = false
                isTimerActive = TempData.audioSleepTimerFutureTime > SystemClock.elapsedRealtime()
            }
        )
    }

    if (showPlaylist) {
        AudioPlaylistPage(
            audioPlaylistVM,
            onDismissRequest = { showPlaylist = false }
        )
    }
} 