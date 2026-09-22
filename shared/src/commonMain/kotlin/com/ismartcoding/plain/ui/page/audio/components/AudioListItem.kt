package com.ismartcoding.plain.ui.page.audio.components

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.platform.checkNotificationPermission
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.models.AudioQueueViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTag
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioListItem(
    item: DAudio, audioVM: AudioViewModel, audioQueueVM: AudioQueueViewModel,
    tagsVM: TagsViewModel, castVM: CastViewModel, tags: List<DTag>,
    dragSelectState: DragSelectState,
    isCurrentlyPlaying: Boolean = false, isInQueue: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val castItems by CastPlayer.items.collectAsState()
    val currentUri by CastPlayer.currentUri.collectAsState()
    val castPlaying by CastPlayer.isPlaying.collectAsState()
    val isCurrentlyPlayingByCast = currentUri == item.path && castPlaying
    val isCurrentItemLoading = castVM.isLoading.value && currentUri == item.path

    val selected = remember(item.id, dragSelectState.selectedIds, audioVM.selectedItem.value) {
        dragSelectState.isSelected(item.id) || audioVM.selectedItem.value?.id == item.id
    }

    Surface(
        modifier = PlainTheme.getCardModifier(selected = selected)
            .combinedClickable(
                onClick = {
                    if (dragSelectState.selectMode) { dragSelectState.select(item.id) }
                    else if (castVM.castMode.value) { castVM.cast(item) }
                    else if (isCurrentlyPlaying) { TempData.audioPlayerVisible.value = true }
                    else {
                        checkNotificationPermission(Res.string.audio_notification_prompt) {
                            scope.launch(Dispatchers.Default) { audioQueueVM.playAsync(item) }
                        }
                    }
                },
                onLongClick = { if (!dragSelectState.selectMode) audioVM.selectedItem.value = item },
            ),
        color = Color.Unspecified,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp, 8.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AudioListItemLeadingIcon(
                item = item, dragSelectState = dragSelectState, castMode = castVM.castMode.value,
                isCurrentItemLoading = isCurrentItemLoading, isCurrentlyPlayingByCast = isCurrentlyPlayingByCast,
                isCurrentlyPlaying = isCurrentlyPlaying,
                coverContent = { AudioCoverOrIcon(path = item.path, modifier = Modifier.size(40.dp)) },
            )
            HorizontalSpace(dp = 12.dp)
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(text = item.title, style = MaterialTheme.typography.listItemTitle())
                VerticalSpace(dp = 8.dp)
                Text(text = item.getSubtitle(), style = MaterialTheme.typography.listItemSubtitle())
                if (tags.isNotEmpty()) {
                    VerticalSpace(dp = 8.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        tags.forEach { tag ->
                            Text(
                                text = "#" + tag.name,
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .padding(end = 8.dp)
                                    .clickable {
                                        if (dragSelectState.selectMode) return@clickable
                                        audioVM.trash.value = false
                                        audioVM.bucketId.value = ""
                                        audioVM.tag.value = tag
                                        scope.launch(Dispatchers.Default) {
                                            audioVM.loadAsync(tagsVM)
                                        }
                                    },
                                style = MaterialTheme.typography.listItemTag(),
                            )
                        }
                    }
                }
            }
            if (!dragSelectState.selectMode) {
                AudioListItemActions(
                    item = item, castMode = castVM.castMode.value,
                    castItems = castItems, isInQueue = isInQueue,
                    onCastToggle = { audio, isInCastQueue ->
                        if (isInCastQueue) CastPlayer.removeItem(audio) else CastPlayer.addItem(audio)
                    },
                    onQueueToggle = { audio, inQueue ->
                        if (inQueue) audioQueueVM.removeAsync(audio.path) else audioQueueVM.addAsync(listOf(audio))
                    },
                )
            }
        }
    }
}
