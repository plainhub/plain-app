package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DAudio
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.PulsatingWave
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTag
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioListItem(
    item: DAudio,
    audioVM: AudioViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    tagsVM: TagsViewModel,
    tags: List<DTag>,
    pagerState: PagerState,
    dragSelectState: DragSelectState,
    isCurrentlyPlaying: Boolean = false,
    isInPlaylist: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var animatingButton by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (animatingButton) 90f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "icon_rotation"
    )

    val iconResource = when {
        isInPlaylist -> R.drawable.playlist_remove
        else -> R.drawable.playlist_add
    }

    val iconColor = when {
        isInPlaylist -> MaterialTheme.colorScheme.red
        else -> MaterialTheme.colorScheme.primary
    }

    val selected = remember(item.id, dragSelectState.selectedIds, audioVM.selectedItem.value) {
        dragSelectState.isSelected(item.id) || audioVM.selectedItem.value?.id == item.id
    }

    Surface(
        modifier = PlainTheme
            .getCardModifier(selected = selected)
            .combinedClickable(
                onClick = {
                    if (dragSelectState.selectMode) {
                        dragSelectState.select(item.id)
                    } else {
                        Permissions.checkNotification(context, R.string.audio_notification_prompt) {
                            scope.launch(Dispatchers.IO) {
                                audioPlaylistVM.playAsync(context, item)
                            }
                        }
                    }
                },
                onLongClick = {
                    if (dragSelectState.selectMode) {
                        return@combinedClickable
                    }
                    audioVM.selectedItem.value = item
                },
            ),
        color = Color.Unspecified,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp, 8.dp, 8.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (dragSelectState.selectMode) {
                    Checkbox(
                        checked = dragSelectState.isSelected(item.id),
                        onCheckedChange = {
                            dragSelectState.select(item.id)
                        }
                    )
                } else if (!isCurrentlyPlaying) {
                    AudioCoverOrIcon(
                        path = item.path,
                        modifier = Modifier.size(40.dp),
                    )
                } else {
                    PulsatingWave(
                        isPlaying = true,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
            }

            HorizontalSpace(dp = 12.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.listItemTitle(),
                )
                VerticalSpace(dp = 8.dp)
                Text(
                    text = item.getSubtitle(),
                    style = MaterialTheme.typography.listItemSubtitle(),
                )
                if (tags.isNotEmpty()) {
                    VerticalSpace(dp = 8.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tags.forEach { tag ->
                            ClickableText(
                                text = AnnotatedString("#" + tag.name),
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .padding(end = 8.dp),
                                style = MaterialTheme.typography.listItemTag(),
                                onClick = {
                                    if (dragSelectState.selectMode) {
                                        return@ClickableText
                                    }
                                    val idx = tagsVM.itemsFlow.value.indexOfFirst { it.id == tag.id }
                                    if (idx != -1) {
                                        scope.launch {
                                            pagerState.scrollToPage(idx + 1)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (!dragSelectState.selectMode) {
                PIconButton(
                    icon = iconResource,
                    tint = iconColor,
                    contentDescription = if (isInPlaylist)
                        stringResource(R.string.remove_from_playlist)
                    else
                        stringResource(R.string.add_to_playlist),
                    modifier = Modifier.rotate(rotation),
                    click = {
                        if (isInPlaylist) {
                            scope.launch(Dispatchers.IO) {
                                animatingButton = true
                                audioPlaylistVM.removeAsync(context, item.path)
                                delay(400)
                                animatingButton = false
                            }
                        } else {
                            scope.launch(Dispatchers.IO) {
                                animatingButton = true
                                audioPlaylistVM.addAsync(context, listOf(item))
                                delay(400)
                                animatingButton = false
                            }
                        }
                    }
                )
            }
        }
    }
}
