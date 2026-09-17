package com.ismartcoding.plain.ui.page.audio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.preferences.AudioSortByPreference
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.MediaTopBar
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.rememberListDragSelectState
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.components.PlaylistNameDialog
import com.ismartcoding.plain.ui.models.AudioHomeViewModel
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.audio.components.ArtistAvatar
import com.ismartcoding.plain.ui.page.audio.components.AudioFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.page.audio.components.AudioListItem
import com.ismartcoding.plain.ui.page.audio.components.PlaylistCoverArtwork
import com.ismartcoding.plain.ui.page.audio.components.ViewAudioBottomSheet
import com.ismartcoding.plain.ui.page.cast.AudioCastPlayerBar
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import com.ismartcoding.plain.ui.page.audioplayer.components.AudioPlayerBar
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AudioHomePage(
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel,
    audioVM: AudioViewModel = viewModel(key = "audioVM") { AudioViewModel() },
    tagsVM: TagsViewModel = viewModel(key = "audioTagsVM") { TagsViewModel() },
    mediaFoldersVM: MediaFoldersViewModel = viewModel(key = "audioFoldersVM") { MediaFoldersViewModel() },
    castVM: CastViewModel = viewModel(key = "audioCastVM") { CastViewModel() },
) {
    val scope = rememberCoroutineScope()
    val homeVM: AudioHomeViewModel = viewModel { AudioHomeViewModel() }
    val audioState = AudioPageState.create(audioVM, tagsVM, mediaFoldersVM)
    val scrollBehavior = audioState.scrollBehavior
    val dragSelectState = audioState.dragSelectState
    val itemsState = audioState.itemsState
    val scrollState = audioState.scrollState
    val tagsState = audioState.tagsState
    val tagsMapState = audioState.tagsMapState
    val isAudioPlaying by audioIsPlayingFlow().collectAsState()
    audioVM.scrollStateMap[0] = scrollState
    var showCreatePlaylist by remember { mutableStateOf(false) }
    // Floating player bar covers list content; measure it and pad the lists.
    val density = LocalDensity.current
    var playerBarClearance by remember { mutableStateOf(0.dp) }

    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            audioVM.loadAsync(tagsVM)
            audioPlaylistVM.loadAsync()
            withIO { mediaFoldersVM.loadAsync() }
            homeVM.loadAsync(audioVM)
            setRefreshState(RefreshContentState.Finished)
        }
    }

    PBackHandler(enabled = dragSelectState.selectMode || castVM.castMode.value || audioVM.showSearchBar.value) {
        when {
            dragSelectState.selectMode -> dragSelectState.exitSelectMode()
            castVM.castMode.value -> castVM.exitCastMode()
            audioVM.showSearchBar.value && (!audioVM.searchActive.value || audioVM.queryText.value.isEmpty()) -> {
                audioVM.exitSearchMode()
                audioVM.showLoading.value = true
                scope.launch(Dispatchers.Default) { audioVM.loadAsync(tagsVM) }
            }
        }
    }

    AudioPageEffects(audioState, audioVM, audioPlaylistVM, tagsVM, mediaFoldersVM)

    val audioTagsMap = remember(tagsMapState, tagsState) {
        tagsMapState.mapValues { entry -> entry.value.mapNotNull { relation -> tagsState.find { it.id == relation.tagId } } }
    }

    ViewAudioBottomSheet(audioVM = audioVM, tagsVM = tagsVM, tagsMapState = tagsMapState, tagsState = tagsState, dragSelectState = dragSelectState, castVM = castVM)
    if (audioVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) { audioVM.showTagsDialog.value = false }
    }
    CastDialog(castVM)

    LaunchedEffect(Unit) {
        homeVM.loadAsync(audioVM)
    }
    LaunchedEffect(itemsState) {
        homeVM.rebuild(itemsState)
    }

    MediaTopBar(
        navController = navController,
        mediaVM = audioVM,
        tagsVM = tagsVM,
        castVM = castVM,
        mediaFoldersVM = mediaFoldersVM,
        dragSelectState = dragSelectState,
        scrollBehavior = scrollBehavior,
        bucketsMap = audioState.bucketsMap,
        itemsState = itemsState,
        scrollToTop = { scope.launch { scrollState.scrollToItem(0) } },
        onSortSelected = { sortBy ->
            scope.launch(Dispatchers.Default) {
                AudioSortByPreference.putAsync(sortBy)
                audioVM.sortBy.value = sortBy
                audioVM.loadAsync(tagsVM)
            }
        },
        onSearchAction = { tv ->
            scope.launch(Dispatchers.Default) {
                audioVM.loadAsync(tv)
            }
        },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                AudioFilesSelectModeBottomActions(audioVM, audioPlaylistVM, tagsVM, tagsState, dragSelectState)
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!audioVM.hasPermission.value) {
                    NeedPermissionColumn(Res.drawable.music, AppFeatureType.FILES.getPermission()!!); return@Column
                }

                PullToRefresh(refreshLayoutState = topRefreshLayoutState, userEnable = !dragSelectState.selectMode, modifier = Modifier.weight(1f)) {
                    // While the top search bar is active or a sidebar filter (folder / tag / trash)
                    // is selected the home sections make no sense:
                    // show the same flat results list as the all-songs page.
                    val sidebarFilterActive = audioVM.trash.value || audioVM.bucketId.value.isNotEmpty() || audioVM.tag.value != null
                    if (audioVM.showSearchBar.value || sidebarFilterActive) {
                        AudioPageList(
                            scrollBehavior, dragSelectState, itemsState, audioVM, audioPlaylistVM,
                            tagsVM, castVM, audioTagsMap, isAudioPlaying, topRefreshLayoutState, paddingValues,
                            extraBottomPadding = playerBarClearance,
                        )
                    } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = scrollState,
                        contentPadding = PaddingValues(bottom = playerBarClearance),
                    ) {
                        item(key = "pills") {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                PFilledButton(
                                    text = stringResource(Res.string.shuffle_play),
                                    icon = painterResource(Res.drawable.shuffle),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        scope.launch {
                                            coIO {
                                                val start = AudioQueueManager.setLibrarySource(startPath = null, shuffle = true)
                                                if (start != null) {
                                                    audioJustPlayWithNotificationCheck(start)
                                                    audioPlaylistVM.onStarted(start)
                                                }
                                            }
                                        }
                                    },
                                )
                                Spacer(Modifier.width(12.dp))
                                POutlinedButton(
                                    text = stringResource(Res.string.all_songs),
                                    icon = painterResource(Res.drawable.music2),
                                    modifier = Modifier.weight(1f),
                                    onClick = { navController.navigate(Routing.AudioAll) },
                                )
                            }
                        }
                        if (homeVM.artists.value.isNotEmpty()) {
                            item(key = "artists_header") {
                                HomeSectionHeader(stringResource(Res.string.artists)) {
                                    navController.navigate(Routing.AudioAll)
                                }
                            }
                            item(key = "artists") {
                                ArtistsRow(homeVM.artists.value) { artist ->
                                    navController.navigate(Routing.ArtistDetail(artist.name))
                                }
                            }
                        }
                        item(key = "playlists_header") {
                            HomeSectionHeader(stringResource(Res.string.playlists), null)
                        }
                        item(key = "playlists") {
                            PlaylistsRow(
                                playlists = homeVM.playlists.value,
                                onNewPlaylist = { showCreatePlaylist = true },
                            ) { pl ->
                                navController.navigate(Routing.PlaylistDetail(pl.first.id))
                            }
                        }
                        item(key = "recent_header") {
                            HomeSectionHeader(stringResource(Res.string.recent)) {
                                navController.navigate(Routing.AudioAll)
                            }
                        }
                        items(homeVM.recentSongs.value.size, key = { homeVM.recentSongs.value[it].path }) { index ->
                            val song = homeVM.recentSongs.value[index]
                            AudioListItem(
                                item = song,
                                audioVM = audioVM,
                                audioPlaylistVM = audioPlaylistVM,
                                tagsVM = tagsVM,
                                castVM = castVM,
                                tags = audioTagsMap[song.id] ?: emptyList(),
                                dragSelectState = dragSelectState,
                                isCurrentlyPlaying = isAudioPlaying && audioPlaylistVM.selectedPath.value == song.path,
                                isInPlaylist = audioPlaylistVM.isInPlaylist(song.path),
                            )
                            VerticalSpace(dp = 8.dp)
                        }
                        item(key = "bottom") { BottomSpace(paddingValues) }
                    }
                    }
                }
            }
            AudioPlayerBar(audioPlaylistVM, castVM, modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { playerBarClearance = with(density) { it.height.toDp() } }, dragSelectState = dragSelectState)
            AudioCastPlayerBar(castVM = castVM, modifier = Modifier.align(Alignment.BottomCenter), dragSelectState = dragSelectState)
        }
    }

    if (showCreatePlaylist) {
        PlaylistNameDialog(
            title = stringResource(Res.string.new_playlist),
            initial = "",
            confirmText = stringResource(Res.string.create),
            onConfirm = { name ->
                showCreatePlaylist = false
                scope.launch(Dispatchers.Default) {
                    AudioQueueManager.createPlaylist(name)
                    homeVM.loadAsync(audioVM)
                }
            },
            onDismiss = { showCreatePlaylist = false },
        )
    }
}

@Composable
private fun HomeSectionHeader(title: String, onViewAll: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.listItemTitle(),
            modifier = Modifier.weight(1f),
        )
        if (onViewAll != null) {
            Text(
                text = stringResource(Res.string.view_all),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewAll)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun ArtistsRow(artists: List<com.ismartcoding.plain.ui.models.AudioHomeArtist>, onArtistClick: (com.ismartcoding.plain.ui.models.AudioHomeArtist) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(artists.size, key = { artists[it].name }) { index ->
            val artist = artists[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onArtistClick(artist) }
                    .padding(4.dp)
                    .width(68.dp),
            ) {
                ArtistAvatar(name = artist.name, gradientIndex = index + 1, size = 64)
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaylistsRow(
    playlists: List<Pair<com.ismartcoding.plain.db.DAudioPlaylist, Int>>,
    onNewPlaylist: () -> Unit,
    onPlaylistClick: (Pair<com.ismartcoding.plain.db.DAudioPlaylist, Int>) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(playlists.size, key = { playlists[it].first.id }) { index ->
            val (pl, count) = playlists[index]
            Column(
                modifier = Modifier
                    .width(132.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlaylistClick(pl to count) }
                    .padding(4.dp),
            ) {
                PlaylistCoverArtwork(
                    gradientIndex = index + 2,
                    modifier = Modifier
                        .size(124.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Text(
                    text = pl.name,
                    style = MaterialTheme.typography.listItemTitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = LocaleHelper.getStringF(Res.string.n_songs, count),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item(key = "new") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(124.dp)
                    .height(124.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable(onClick = onNewPlaylist),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.plus),
                    contentDescription = stringResource(Res.string.new_playlist),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = stringResource(Res.string.new_playlist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
