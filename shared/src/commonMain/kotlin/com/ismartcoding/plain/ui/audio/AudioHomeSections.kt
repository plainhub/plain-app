package com.ismartcoding.plain.ui.audio

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.models.AudioArtist
import com.ismartcoding.plain.models.AudioPlaylist
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.artists
import com.ismartcoding.plain.i18n.playlists
import com.ismartcoding.plain.i18n.recent
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.audio.components.ArtistsRow
import com.ismartcoding.plain.ui.audio.components.AudioListItem
import com.ismartcoding.plain.ui.audio.components.HomeQuickActions
import com.ismartcoding.plain.ui.audio.components.HomeSectionHeader
import com.ismartcoding.plain.ui.audio.components.PlaylistsRow
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AudioHomeSections(
    homeVM: AudioHomeViewModel,
    audioVM: AudioViewModel,
    audioQueueVM: AudioQueueViewModel,
    tagsVM: TagsViewModel,
    castVM: CastViewModel,
    audioTagsMap: Map<String, List<Tag>>,
    isAudioPlaying: Boolean,
    scrollState: LazyListState,
    dragSelectState: DragSelectState,
    playerBarClearance: Dp,
    paddingValues: PaddingValues,
    onShuffleAll: () -> Unit,
    onViewAllItems: () -> Unit,
    onArtistsViewAll: () -> Unit,
    onArtistClick: (AudioArtist) -> Unit,
    onPlaylistClick: (AudioPlaylist) -> Unit,
) {
    val artists = homeVM.artists.value
    val playlists = homeVM.playlists.value
    val recent = homeVM.recentItems.value

    LazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState) {
        item(key = "quick_actions") {
            HomeQuickActions(onShuffleAll = onShuffleAll, onViewAllItems = onViewAllItems)
        }
        if (artists.isNotEmpty()) {
            item(key = "artists_header") {
                HomeSectionHeader(stringResource(Res.string.artists), onArtistsViewAll)
            }
            item(key = "artists") {
                ArtistsRow(artists.take(10), onArtistClick)
            }
        }
        if (playlists.isNotEmpty()) {
            item(key = "playlists_header") {
                HomeSectionHeader(stringResource(Res.string.playlists), null)
            }
            item(key = "playlists") {
                PlaylistsRow(playlists, onPlaylistClick)
            }
        }
        item(key = "recent_header") {
            HomeSectionHeader(stringResource(Res.string.recent), null)
        }
        items(recent.size, key = { recent[it].path }) { index ->
            val item = recent[index]
            AudioListItem(
                item = item,
                audioVM = audioVM,
                audioQueueVM = audioQueueVM,
                tagsVM = tagsVM,
                castVM = castVM,
                tags = audioTagsMap[item.id] ?: emptyList(),
                dragSelectState = dragSelectState,
                isCurrentlyPlaying = isAudioPlaying && audioQueueVM.selectedMediaId.value == item.id,
                isInQueue = audioQueueVM.isInQueue(item.id),
            )
            VerticalSpace(dp = 8.dp)
        }
        item(key = "bottom") {
            VerticalSpace(dp = maxOf(playerBarClearance, 40.dp + paddingValues.calculateBottomPadding()))
        }
    }
}
