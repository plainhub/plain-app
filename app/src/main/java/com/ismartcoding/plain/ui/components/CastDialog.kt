package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.features.StartHttpServerEvent
import com.ismartcoding.plain.preference.WebPreference
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.models.CastViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CastDialog(viewModel: CastViewModel) {
    if (!viewModel.showCastDialog.value) {
        return
    }
    val itemsState by viewModel.itemsFlow.collectAsState()
    var loadingTextId by remember {
        mutableIntStateOf(R.string.searching_devices)
    }
    val scope = rememberCoroutineScope()
    val onDismiss = {
        viewModel.showCastDialog.value = false
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            viewModel.searchAsync(context)
        }
        coIO {
            delay(5000)
            if (itemsState.isEmpty()) {
                loadingTextId = R.string.no_devices_found
            }
        }
    }
    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.select_a_device),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (itemsState.isNotEmpty()) {
                LazyColumn(modifier = Modifier.defaultMinSize(minHeight = 100.dp)) {
                    items(itemsState) { m ->
                        PDialogListItem(modifier = Modifier.clickable {
                            viewModel.selectDevice(m)
                            viewModel.enterCastMode()
                            scope.launch(Dispatchers.IO) {
                                if(!TempData.webEnabled) {
                                    WebPreference.putAsync(context, true)
                                    sendEvent(StartHttpServerEvent())
                                }
                            }
                            onDismiss()
                        }, title = m.description?.device?.friendlyName ?: "", showMore = true)
                    }
                }
            } else {
                Text(
                    text = stringResource(id = loadingTextId),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onDismiss()
            }) {
                Text(text = stringResource(id = R.string.close))
            }
        },
        dismissButton = {
        },
    )
}