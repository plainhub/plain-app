package com.ismartcoding.plain.ui.page.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.reorderable.ReorderableItem
import com.ismartcoding.plain.ui.base.reorderable.rememberReorderableLazyListState
import com.ismartcoding.plain.ui.components.PulsatingWave
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.theme.cardBackgroundActive
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.circleBackground
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.red
import com.ismartcoding.plain.ui.theme.secondaryTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlaylistPage(
    audioPlaylistVM: AudioPlaylistViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val isAudioPlaying by AudioPlayer.isPlayingFlow.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        scope.launch(Dispatchers.IO) {
            audioPlaylistVM.reorder(context, from.index, to.index)
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.clear_all)) },
            text = { Text(stringResource(R.string.clear_all_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            audioPlaylistVM.clearAsync(context)
                            showClearConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    PModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column {
            PBottomSheetTopAppBar(
                titleContent = {
                    Text(
                        text = if (audioPlaylistVM.playlistItems.value.isNotEmpty())
                            LocaleHelper.getStringF(R.string.playlist_title, "total", audioPlaylistVM.playlistItems.value.size)
                        else stringResource(R.string.playlist),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    if (audioPlaylistVM.playlistItems.value.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.delete_forever),
                                contentDescription = "Clear playlist",
                                tint = MaterialTheme.colorScheme.red
                            )
                        }
                    }
                }
            )

            if (audioPlaylistVM.playlistItems.value.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.empty_playlist),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondaryTextColor
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.drag_number_to_reorder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondaryTextColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    )

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        itemsIndexed(audioPlaylistVM.playlistItems.value, { _, item -> item.path }) { index, audio ->
                            val isPlaying = isAudioPlaying && audioPlaylistVM.selectedPath.value == audio.path

                            ReorderableItem(reorderableLazyListState, key = audio.path) { isDragging ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            Permissions.checkNotification(
                                                context,
                                                R.string.audio_notification_prompt
                                            ) {
                                                AudioPlayer.justPlay(context, audio)
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPlaying)
                                            MaterialTheme.colorScheme.cardBackgroundActive
                                        else MaterialTheme.colorScheme.cardBackgroundNormal
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isPlaying)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.circleBackground
                                                )
                                                .then(with(reorderableLazyListState) { Modifier.draggableHandle() }),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isPlaying) {
                                                PulsatingWave(
                                                    isPlaying = true,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.secondaryTextColor
                                                )
                                            }
                                        }

                                        HorizontalSpace(16.dp)

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = audio.title,
                                                style = MaterialTheme.typography.listItemTitle()
                                            )

                                            VerticalSpace(4.dp)
                                            Text(
                                                text = audio.getSubtitle(),
                                                style = MaterialTheme.typography.listItemSubtitle()
                                            )
                                        }

                                        PIconButton(
                                            icon = R.drawable.playlist_remove,
                                            tint = MaterialTheme.colorScheme.red,
                                            contentDescription = stringResource(R.string.remove_from_playlist),
                                            click = {
                                                scope.launch(Dispatchers.IO) {
                                                    audioPlaylistVM.removeAsync(context, audio.path)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
