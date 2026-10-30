package com.ismartcoding.plain.ui.audio.components

import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.has

import com.ismartcoding.plain.features.audio.getSubtitle
import com.ismartcoding.plain.models.Audio as AudioTrack
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
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.permissions.checkNotificationPermission
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.audio.AudioViewModel
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTag
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioListItem(
    item: AudioTrack, audioVM: AudioViewModel, audioQueueVM: AudioQueueViewModel,
    tagsVM: TagsViewModel, castVM: CastViewModel, tags: List<Tag>,
    dragSelectState: DragSelectState,
    isCurrentlyPlaying: Boolean = false, isInQueue: Boolean = false,
) {
    val audioOutput by com.ismartcoding.plain.features.audio.AudioCommands.output.collectAsState()
    val current = isCurrentlyPlaying && audioOutput.source.key == audioVM.source.key
    val scope = rememberCoroutineScope()
    val castItems by castVM.items.collectAsState()
    val currentUri by castVM.currentUri.collectAsState()
    val castPlaying by castVM.isPlaying.collectAsState()
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
                    else if (current) { audioQueueVM.audioPlayerVisible.value = true }
                    else {
                        checkNotificationPermission(Res.string.audio_notification_prompt) {
                            scope.launch(Dispatchers.Default) { audioQueueVM.enqueueAndPlayAsync(item) }
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
                isCurrentlyPlaying = current,
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
                        if (isInCastQueue) castVM.removeCastItem(audio) else castVM.addCastItem(audio)
                    },
                    onQueueToggle = { audio, inQueue ->
                        if (inQueue) audioQueueVM.removeAsync(audio.id) else audioQueueVM.addAsync(listOf(audio))
                    },
                )
            }
        }
    }
}
