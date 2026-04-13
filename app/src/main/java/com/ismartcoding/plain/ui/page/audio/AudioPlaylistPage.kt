package com.ismartcoding.plain.ui.page.audio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.reorderable.ReorderableItem
import com.ismartcoding.plain.ui.base.reorderable.rememberReorderableLazyListState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.theme.red
import com.ismartcoding.plain.ui.theme.secondaryTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlaylistPage(audioPlaylistVM: AudioPlaylistViewModel, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isAudioPlaying by AudioPlayer.isPlayingFlow.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        scope.launch(Dispatchers.IO) { audioPlaylistVM.reorder(context, from.index, to.index) }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.clear_all)) },
            text = { Text(stringResource(R.string.clear_all_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = { scope.launch(Dispatchers.IO) { audioPlaylistVM.clearAsync(context); showClearConfirmDialog = false } },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirmDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    PModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column {
            PBottomSheetTopAppBar(
                title = if (audioPlaylistVM.playlistItems.value.isNotEmpty())
                    LocaleHelper.getStringF(R.string.playlist_title, "total", audioPlaylistVM.playlistItems.value.size)
                else stringResource(R.string.playlist),
                subtitle = if (audioPlaylistVM.playlistItems.value.isEmpty()) "" else stringResource(R.string.drag_number_to_reorder_list),
                actions = {
                    if (audioPlaylistVM.playlistItems.value.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(painter = painterResource(R.drawable.delete_forever), contentDescription = "Clear", tint = MaterialTheme.colorScheme.red)
                        }
                    }
                }
            )
            VerticalSpace(8.dp)
            if (audioPlaylistVM.playlistItems.value.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.empty_playlist), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondaryTextColor)
                }
            } else {
                LazyColumn(
                    state = lazyListState, modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    itemsIndexed(audioPlaylistVM.playlistItems.value, { _, item -> item.path }) { index, audio ->
                        val isPlaying = isAudioPlaying && audioPlaylistVM.selectedPath.value == audio.path
                        ReorderableItem(reorderableLazyListState, key = audio.path) { isDragging ->
                            AudioPlaylistItemRow(
                                audio = audio, index = index, isPlaying = isPlaying,
                                audioPlaylistVM = audioPlaylistVM, scope = scope
                            )
                        }
                    }
                }
            }
        }
    }
}
