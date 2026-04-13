package com.ismartcoding.plain.ui.page.cast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.reorderable.ReorderableItem
import com.ismartcoding.plain.ui.base.reorderable.rememberReorderableLazyListState
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.theme.cardBackgroundActive
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.red
import com.ismartcoding.plain.ui.theme.secondaryTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioCastPlaylistPage(castVM: CastViewModel, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val castItems by CastPlayer.items.collectAsState()
    val currentUri by CastPlayer.currentUri.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        scope.launch(Dispatchers.IO) { CastPlayer.reorderItems(from.index, to.index) }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.clear_all)) },
            text = { Text(stringResource(R.string.clear_all_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = { scope.launch(Dispatchers.IO) { CastPlayer.clearItems(); showClearConfirmDialog = false } },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirmDialog = false }) { Text(stringResource(R.string.cancel)) } })
    }

    PModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column {
            PBottomSheetTopAppBar(
                title = if (castItems.isNotEmpty()) LocaleHelper.getStringF(R.string.playlist_title, "total", castItems.size) else stringResource(R.string.cast_playlist),
                subtitle = if (castItems.isEmpty()) "" else stringResource(R.string.drag_number_to_reorder_list),
                actions = {
                    if (castItems.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(painter = painterResource(R.drawable.delete_forever), contentDescription = "Clear playlist", tint = MaterialTheme.colorScheme.red)
                        }
                    }
                })

            if (castItems.isEmpty()) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.cast_playlist_empty), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondaryTextColor)
                }
            } else {
                LazyColumn(state = lazyListState, modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), contentPadding = PaddingValues(bottom = 96.dp)) {
                    itemsIndexed(castItems, { _, item -> item.path }) { index, audio ->
                        val isPlaying = currentUri == audio.path
                        ReorderableItem(reorderableLazyListState, key = audio.path) { isDragging ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { castVM.cast(audio) },
                                colors = CardDefaults.cardColors(containerColor = if (isPlaying) MaterialTheme.colorScheme.cardBackgroundActive else MaterialTheme.colorScheme.cardBackgroundNormal)
                            ) {
                                with(this@ReorderableItem) {
                                    AudioCastPlaylistItemContent(
                                        item = audio, index = index, isPlaying = isPlaying,
                                        isLoading = castVM.isLoading.value && isPlaying, onRemove = { scope.launch(Dispatchers.IO) { CastPlayer.removeItemAt(index) } })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
