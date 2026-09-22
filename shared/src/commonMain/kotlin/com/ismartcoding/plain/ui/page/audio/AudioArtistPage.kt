package com.ismartcoding.plain.ui.page.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatDuration
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.platform.audioPause
import com.ismartcoding.plain.platform.audioPlay
import com.ismartcoding.plain.preferences.AudioSortByPreference
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.dragselect.listDragSelect
import com.ismartcoding.plain.ui.base.dragselect.rememberListDragSelectState
import com.ismartcoding.plain.ui.models.AudioQueueViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.audio.components.ArtistAvatar
import com.ismartcoding.plain.ui.page.audio.components.AudioFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.page.audioplayer.components.AudioPlayerBar
import com.ismartcoding.plain.ui.page.cast.AudioCastPlayerBar
import com.ismartcoding.plain.ui.page.audio.components.AudioListItem
import com.ismartcoding.plain.ui.page.audio.components.ViewAudioBottomSheet
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Artist detail: hero header plus the artist's tracks, play-all / shuffle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioArtistPage(
    navController: NavHostController,
    artistName: String,
    audioQueueVM: AudioQueueViewModel,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    castVM: CastViewModel,
) {
    val scope = rememberCoroutineScope()
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()

    val isPlaying by audioIsPlayingFlow().collectAsState()
    var items by remember { mutableStateOf<List<DAudio>>(listOf()) }
    val scrollState = rememberLazyListState()
    val dragSelectState = rememberListDragSelectState({ scrollState })
    // Floating player bar clearance, measured live like on the other pages.
    val density = LocalDensity.current
    var playerBarClearance by remember { mutableStateOf(0.dp) }

    LaunchedEffect(Unit) {
        tagsVM.dataType.value = audioVM.dataType
    }
    LaunchedEffect(artistName) {
        items = withIO {
            searchMedia(DataType.AUDIO, artistName, 500, 0, AudioSortByPreference.getValueAsync())
        }.filterIsInstance<DAudio>().filter { it.artist == artistName }
    }

    // Play-all fills the manual queue with exactly this artist's tracks;
    // while that set matches, the play button toggles pause/resume.
    val artistPaths = remember(items) { items.map { it.path }.toSet() }
    val contextActive = items.isNotEmpty() && audioQueueVM.queuedPaths.value == artistPaths

    val playAll: (Boolean) -> Unit = { shuffle ->
        scope.launch {
            val list = if (shuffle) items.shuffled() else items
            withIO {
                AudioQueueManager.clearQueue()
                AudioQueueManager.enqueue(list.map { it.toPlaylistAudio() })
            }
            val first = list.firstOrNull()?.toPlaylistAudio() ?: return@launch
            audioJustPlayWithNotificationCheck(first)
            audioQueueVM.onStarted(first)
        }
    }

    PBackHandler(enabled = dragSelectState.selectMode) {
        dragSelectState.exitSelectMode()
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                title = "",
                navController = navController,
            )
        },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                AudioFilesSelectModeBottomActions(audioVM, audioQueueVM, tagsVM, tagsState, dragSelectState)
            }
        },
    ) { paddingValues ->
        // Only the top inset goes on the container; the player bar carries its
        // own navigationBarsPadding and must sit flush at the screen bottom
        // (same pattern as AudioAllPage).
        Box(Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    // Two header items (hero, acts) precede the track rows.
                    .listDragSelect(items = items, state = dragSelectState, itemIndexOffset = 2),
                state = scrollState,
            ) {
                item(key = "hero") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtistAvatar(name = artistName, artworkPath = items.firstOrNull()?.path, size = 96.dp)
                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.listItemTitle(),
                                maxLines = 2,
                            )
                            Text(
                                text = pluralStringResource(Res.plurals.items, items.size, items.size),
                                style = MaterialTheme.typography.listItemSubtitle(),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                item(key = "acts") {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        PFilledButton(
                            text = stringResource(Res.string.play_all),
                            icon = painterResource(if (contextActive && isPlaying) Res.drawable.pause else Res.drawable.play_arrow),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (contextActive) {
                                    if (isPlaying) audioPause() else audioPlay()
                                } else {
                                    playAll(false)
                                }
                            },
                        )
                        Spacer(Modifier.width(12.dp))
                        POutlinedButton(
                            text = stringResource(Res.string.shuffle_play),
                            icon = painterResource(Res.drawable.shuffle),
                            modifier = Modifier.weight(1f),
                            onClick = { playAll(true) },
                        )
                    }
                }
                items(items.size, key = { items[it].path }) { index ->
                    val item = items[index]
                    AudioListItem(
                        item = item,
                        audioVM = audioVM,
                        audioQueueVM = audioQueueVM,
                        tagsVM = tagsVM,
                        castVM = castVM,
                        tags = emptyList(),
                        dragSelectState = dragSelectState,
                        isCurrentlyPlaying = audioQueueVM.selectedPath.value == item.path,
                        isInQueue = audioQueueVM.isInQueue(item.path),
                    )
                    VerticalSpace(8.dp)
                }
                item(key = "bottom") {
                    VerticalSpace(dp = maxOf(playerBarClearance, 40.dp + paddingValues.calculateBottomPadding()))
                }
            }
            AudioPlayerBar(
                audioQueueVM, castVM,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { playerBarClearance = with(density) { it.height.toDp() } },
                dragSelectState = dragSelectState,
            )
            AudioCastPlayerBar(castVM = castVM, modifier = Modifier.align(Alignment.BottomCenter), dragSelectState = dragSelectState)
        }
    }

    ViewAudioBottomSheet(
        audioVM = audioVM,
        tagsVM = tagsVM,
        tagsMapState = tagsMapState,
        tagsState = tagsState,
        dragSelectState = dragSelectState,
        castVM = castVM,
    )
}
