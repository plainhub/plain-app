package com.ismartcoding.plain.ui.page.cast

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.StartHttpServerEvent
import com.ismartcoding.plain.preference.WebPreference
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.models.CastViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CastDialog(castVM: CastViewModel) {
    if (!castVM.showCastDialog.value) {
        return
    }
    val itemsState by castVM.itemsFlow.collectAsState()
    var loadingTextId by remember {
        mutableIntStateOf(R.string.searching_devices)
    }
    val scope = rememberCoroutineScope()
    val onDismiss = {
        castVM.showCastDialog.value = false
    }
    val context = LocalContext.current
    var job by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                job?.cancel()
                if (itemsState.isEmpty()) {
                    job = coIO {
                        castVM.searchAsync(context)
                    }
                }
                delay(5000)
                if (itemsState.isEmpty()) {
                    loadingTextId = R.string.no_devices_found
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            job?.cancel()
        }
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
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
                            castVM.selectDevice(m)
                            castVM.enterCastMode()
                            AudioPlayer.pause()
                            scope.launch(Dispatchers.IO) {
                                val webEnabled = WebPreference.getAsync(context)
                                if (!webEnabled) {
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