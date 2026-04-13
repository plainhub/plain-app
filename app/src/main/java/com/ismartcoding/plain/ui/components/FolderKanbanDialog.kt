package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.events.FolderKanbanSelectEvent
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.FolderOption
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderKanbanDialog(filesVM: FilesViewModel, onDismiss: () -> Unit = {}) {
    val context = LocalContext.current
    val options = remember { mutableStateListOf<FolderOption>() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recentsText = stringResource(R.string.recents)
    val internalStorageText = stringResource(R.string.internal_storage)
    val sdcardText = stringResource(R.string.sdcard)
    val usbStorageText = stringResource(R.string.usb_storage)
    val fileTransferAssistantText = stringResource(R.string.app_data)

    LaunchedEffect(Unit) {
        val items = buildFolderOptions(context, filesVM, recentsText, internalStorageText, sdcardText, usbStorageText, fileTransferAssistantText)
        options.clear()
        options.addAll(items)
    }

    PModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column {
            PBottomSheetTopAppBar(title = stringResource(R.string.folders))
            if (options.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxSize()) {
                    item { TopSpace() }
                    items(options) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { sendEvent(FolderKanbanSelectEvent(item)); onDismiss() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = item.isChecked, onClick = { sendEvent(FolderKanbanSelectEvent(item)); onDismiss() })
                            HorizontalSpace(8.dp)
                            Text(text = item.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            if (item.isFavoriteFolder) {
                                PIconButton(icon = R.drawable.delete_forever, tint = MaterialTheme.colorScheme.red, contentDescription = stringResource(R.string.delete), click = {
                                    DialogHelper.confirmToDelete {
                                        scope.launch(Dispatchers.IO) { FavoriteFoldersPreference.removeAsync(context, item.fullPath); options.remove(item) }
                                    }
                                })
                            }
                        }
                    }
                    item { BottomSpace() }
                }
            } else {
                NoDataColumn(loading = true)
            }
        }
    }
}
