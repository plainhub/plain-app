package com.ismartcoding.plain.ui.page.cast

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.events.StartHttpServerEvent
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.models.CastViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastDialog(castVM: CastViewModel) {
    if (!castVM.showCastDialog.value) return
    val itemsState by castVM.itemsFlow.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val hasDevices = itemsState.isNotEmpty()
    val heroHeight by animateDpAsState(if (hasDevices) 104.dp else 156.dp, label = "castHeroHeight")
    var job by remember { mutableStateOf<Job?>(null) }
    val onDismiss = { castVM.showCastDialog.value = false }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                job?.cancel()
                if (itemsState.isEmpty()) job = coIO { castVM.searchAsync(context) }
                delay(5000)
            }
        }
    }
    LaunchedEffect(hasDevices) {
        if (hasDevices) sheetState.expand()
    }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }

    PModalBottomSheet(
        modifier = Modifier.defaultMinSize(minHeight = 360.dp),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().animateContentSize().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PBottomSheetTopAppBar(
                title = stringResource(if (hasDevices) R.string.cast_select_screen else R.string.cast_searching_for_screen),
                subtitle = stringResource(if (hasDevices)  R.string.cast_dialog_hint else R.string.cast_looking_for_devices),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SearchingScreenAnimation(Modifier.fillMaxWidth().height(heroHeight))
            }
            if (hasDevices) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(itemsState, key = { it.hostAddress }) { device ->
                        CastDeviceCard(
                            title = device.description?.device?.friendlyName ?: device.hostAddress,
                            subtitle = device.hostAddress,
                            onClick = {
                                castVM.selectDevice(device)
                                castVM.enterCastMode()
                                AudioPlayer.pause()
                                scope.launch(Dispatchers.IO) {
                                    if (!WebPreference.getAsync(context)) {
                                        WebPreference.putAsync(context, true)
                                        sendEvent(StartHttpServerEvent())
                                    }
                                }
                                onDismiss()
                            },
                        )
                    }
                }
            }
            Tips(stringResource(R.string.cast_dialog_wireless_cast_tip))
            BottomSpace()
        }
    }
}

