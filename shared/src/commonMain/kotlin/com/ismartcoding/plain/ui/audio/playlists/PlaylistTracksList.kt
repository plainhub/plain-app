package com.ismartcoding.plain.ui.audio.playlists

import com.ismartcoding.plain.models.Audio as AudioTrack
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.audio.AudioViewModel
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.audio.components.AudioListItem
import com.ismartcoding.plain.ui.audio.playlists.components.PlaylistHeaderRow
import com.ismartcoding.plain.ui.audio.playlists.components.PlaylistActionsRow
import androidx.compose.ui.unit.dp

@Composable
internal fun PlaylistTracksList(
    name: String,
    itemCount: Int,
    candidatePaths: List<String>,
    gradientIndex: Int,
    rowAudios: List<AudioTrack>,
    audioVM: AudioViewModel,
    audioQueueVM: AudioQueueViewModel,
    tagsVM: TagsViewModel,
    castVM: CastViewModel,
    dragSelectState: DragSelectState,
    isPlaying: Boolean,
    isPlayingContext: Boolean,
    state: LazyListState,
    bottomClearance: Dp,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, state = state) {
        item(key = "hero") {
            PlaylistHeaderRow(name = name, itemCount = itemCount, candidatePaths = candidatePaths, gradientIndex = gradientIndex)
        }
        item(key = "acts") {
            PlaylistActionsRow(enabled = itemCount > 0, isPlaying = isPlayingContext, onPlayAll = onPlayAll, onShuffle = onShuffle)
        }
        items(rowAudios.size, key = { rowAudios[it].id }) { index ->
            val item = rowAudios[index]
            AudioListItem(
                item = item, audioVM = audioVM, audioQueueVM = audioQueueVM,
                tagsVM = tagsVM, castVM = castVM, tags = emptyList(),
                dragSelectState = dragSelectState,
                isCurrentlyPlaying = isPlaying && audioQueueVM.selectedMediaId.value == item.id,
                isInQueue = audioQueueVM.isInQueue(item.id),
            )
            VerticalSpace(8.dp)
        }
        item(key = "bottom") { VerticalSpace(dp = bottomClearance) }
    }
}
