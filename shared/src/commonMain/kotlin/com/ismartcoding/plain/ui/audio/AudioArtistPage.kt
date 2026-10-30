package com.ismartcoding.plain.ui.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.ismartcoding.plain.features.media.artistQuery
import com.ismartcoding.plain.models.Audio as AudioTrack
import com.ismartcoding.plain.preferences.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.platform.media.audioIsPlayingFlow
import com.ismartcoding.plain.platform.media.audioPause
import com.ismartcoding.plain.platform.media.audioPlay
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.media.MediaTopBar
import com.ismartcoding.plain.ui.components.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.listDragSelect
import com.ismartcoding.plain.ui.base.dragselect.rememberListDragSelectState
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.files.MediaFoldersViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.audio.components.ArtistAvatar
import com.ismartcoding.plain.ui.audio.components.AudioFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.audio.player.components.AudioPlayerBar
import com.ismartcoding.plain.ui.casting.CastDialog
import com.ismartcoding.plain.ui.casting.CastQueueFab
import com.ismartcoding.plain.ui.audio.components.AudioListItem
import com.ismartcoding.plain.ui.audio.components.ViewAudioBottomSheet
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.cast as ui_drawable_cast
import com.ismartcoding.plain.ui.resources.pause as ui_drawable_pause
import com.ismartcoding.plain.ui.resources.play_arrow as ui_drawable_play_arrow
import com.ismartcoding.plain.ui.resources.shuffle as ui_drawable_shuffle
import com.ismartcoding.plain.ui.resources.sort as ui_drawable_sort
import com.ismartcoding.plain.i18n.sort

/** Artist detail: hero header plus the artist's tracks, play-all / shuffle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioArtistPage(
    navController: NavHostController,
    artistName: String,
    audioQueueVM: AudioQueueViewModel,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    mediaFoldersVM: MediaFoldersViewModel,
    castVM: CastViewModel,
) {
    val scope = rememberCoroutineScope()
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()

    val isPlaying by audioIsPlayingFlow().collectAsState()
    var items by remember { mutableStateOf<List<AudioTrack>>(listOf()) }
    val scrollState = rememberLazyListState()
    val dragSelectState = rememberListDragSelectState({ scrollState })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        scrollState.firstVisibleItemIndex > 0 && !dragSelectState.selectMode
    })
    // Floating player bar clearance, measured live like on the other pages.
    val density = LocalDensity.current
    var playerBarClearance by remember { mutableStateOf(0.dp) }

    LaunchedEffect(Unit) {
        tagsVM.dataType.value = audioVM.dataType
    }
    // Reload on sort changes so the shared sort dialog re-sorts the tracks.
    val sortBy = audioVM.sortBy.value
    LaunchedEffect(artistName, sortBy) {
        items = withIO {
            audioVM.source.catalog.artistTracks(artistName, sortBy)
        }
    }

    // Play-all fills the manual queue with exactly this artist's tracks;
    // while that set matches, the play button toggles pause/resume.
    val audioOutput by com.ismartcoding.plain.features.audio.AudioCommands.output.collectAsState()
    val contextActive = audioOutput.source.key == audioVM.source.key && items.isNotEmpty() && audioQueueVM.queuedMediaIds.value == items.map { it.id }.toSet()

    val playAll: (Boolean) -> Unit = { shuffle ->
        scope.launchSafe {
            audioQueueVM.playQueryAsync(artistQuery(artistName), shuffle = shuffle)
        }
    }

    PBackHandler(enabled = dragSelectState.selectMode || castVM.castMode.value) {
        when {
            dragSelectState.selectMode -> dragSelectState.exitSelectMode()
            castVM.castMode.value -> castVM.exitCastMode()
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
    CastDialog(castVM)

    MediaTopBar(
        navController = navController,
        mediaVM = audioVM,
        tagsVM = tagsVM,
        castVM = castVM,
        mediaFoldersVM = mediaFoldersVM,
        dragSelectState = dragSelectState,
        scrollBehavior = scrollBehavior,
        // Sub-page with its own focused list: no folder/tag sidebar.
        showSidebar = false,
        bucketsMap = emptyMap(),
        itemsState = items,
        scrollToTop = { scope.launch { scrollState.scrollToItem(0) } },
        onSortSelected = { sortBy ->
            scope.launch {
                Prefs.set(PrefKeys.AUDIO_SORT_BY, sortBy.name)
                audioVM.sortBy.value = sortBy
            }
        },
        onSearchAction = { },
        navigationIcon = {
            NavigationBackIcon { navController.navigateUp() }
        },
        topBarActions = {
            PIconButton(
                icon = UiRes.drawable.ui_drawable_sort,
                contentDescription = stringResource(Res.string.sort),
                click = { audioVM.showSortAndBrowseDialog.value = true },
            )
            PIconButton(
                icon = UiRes.drawable.ui_drawable_cast,
                contentDescription = stringResource(Res.string.cast_mode),
                click = { castVM.showCastDialog.value = true },
            )
        },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                AudioFilesSelectModeBottomActions(audioVM, audioQueueVM, tagsVM, tagsState, dragSelectState, items)
            }
        },
    ) { paddingValues ->
        // Only the top inset goes on the container; the player bar carries its
        // own navigationBarsPadding and must sit flush at the screen bottom
        // (same pattern as AudioAllPage).
        Box(Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
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
                            icon = painterResource(if (contextActive && isPlaying) UiRes.drawable.ui_drawable_pause else UiRes.drawable.ui_drawable_play_arrow),
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
                            icon = painterResource(UiRes.drawable.ui_drawable_shuffle),
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
                        isCurrentlyPlaying = audioQueueVM.selectedMediaId.value == item.id,
                        isInQueue = audioQueueVM.isInQueue(item.id),
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
            CastQueueFab(castVM = castVM, modifier = Modifier.align(Alignment.BottomEnd), dragSelectState = dragSelectState)
        }
    }
}
