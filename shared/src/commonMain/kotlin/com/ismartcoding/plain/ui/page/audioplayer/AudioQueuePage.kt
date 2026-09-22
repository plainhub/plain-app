package com.ismartcoding.plain.ui.page.audioplayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.cancel
import com.ismartcoding.plain.i18n.clear_all
import com.ismartcoding.plain.i18n.clear_all_confirm
import com.ismartcoding.plain.i18n.confirm
import com.ismartcoding.plain.i18n.delete_forever
import com.ismartcoding.plain.i18n.drag_number_to_reorder_list
import com.ismartcoding.plain.i18n.empty_queue
import com.ismartcoding.plain.i18n.ok
import com.ismartcoding.plain.i18n.queue
import com.ismartcoding.plain.i18n.queue_title
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.reorderable.ReorderableItem
import com.ismartcoding.plain.ui.base.reorderable.rememberReorderableLazyListState
import com.ismartcoding.plain.ui.helpers.confirmActionAsync
import com.ismartcoding.plain.ui.models.AudioQueueViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioQueuePage(audioQueueVM: AudioQueueViewModel, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isAudioPlaying by audioIsPlayingFlow().collectAsState()
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        scope.launch(Dispatchers.Default) { audioQueueVM.reorder(from.index, to.index) }
    }

    PModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column {
            PBottomSheetTopAppBar(
                title = if (audioQueueVM.queueCount.value > 0)
                    LocaleHelper.getStringF(Res.string.queue_title, audioQueueVM.queueCount.value)
                else stringResource(Res.string.queue),
                subtitle = if (audioQueueVM.canReorder.value && audioQueueVM.queueItems.value.isNotEmpty()) stringResource(Res.string.drag_number_to_reorder_list) else "",
                actions = {
                    if (audioQueueVM.queueItems.value.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                confirmActionAsync(
                                    Res.string.clear_all,
                                    Res.string.clear_all_confirm,
                                    callback = {
                                        scope.launch { audioQueueVM.clearAsync() }
                                    },
                                    danger = true
                                )
                            }
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.delete_forever),
                                contentDescription = stringResource(Res.string.clear_all),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
            VerticalSpace(8.dp)
            if (audioQueueVM.queueItems.value.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(Res.string.empty_queue), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = lazyListState, modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    itemsIndexed(audioQueueVM.queueItems.value, { _, item -> item.path }) { index, audio ->
                        val isPlaying = isAudioPlaying && audioQueueVM.selectedPath.value == audio.path
                        ReorderableItem(reorderableLazyListState, key = audio.path) { isDragging ->
                            AudioQueueItemRow(
                                audio = audio, index = index, isPlaying = isPlaying,
                                canReorder = audioQueueVM.canReorder.value,
                                canRemove = audioQueueVM.isInQueue(audio.path),
                                audioQueueVM = audioQueueVM, scope = scope
                            )
                        }
                    }
                    item(key = "loadMore") {
                        if (!audioQueueVM.noMore.value) {
                            LaunchedEffect(audioQueueVM.queueItems.value.size) {
                                scope.launch(Dispatchers.Default) { audioQueueVM.moreAsync() }
                            }
                        }
                        LoadMoreRefreshContent(audioQueueVM.noMore.value)
                    }
                }
            }
        }
    }
}
