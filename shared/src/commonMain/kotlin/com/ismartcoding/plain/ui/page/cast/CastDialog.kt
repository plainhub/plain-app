package com.ismartcoding.plain.ui.page.cast

import com.ismartcoding.plain.i18n.*

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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.dlna.sender.DlnaDeviceScanner
import com.ismartcoding.plain.events.StartHttpServerEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.audioPause
import com.ismartcoding.plain.preferences.ServicePreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.models.CastViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastDialog(castVM: CastViewModel, onDeviceSelected: (() -> Unit)? = null) {
    if (!castVM.showCastDialog.value) return
    val devices by DlnaDeviceScanner.devices.collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasDevices = devices.isNotEmpty()
    val onDismiss = { castVM.showCastDialog.value = false }

    LaunchedEffect(Unit) {
        DlnaDeviceScanner.start()
        sheetState.expand()
    }
    DisposableEffect(Unit) {
        onDispose { DlnaDeviceScanner.stop() }
    }

    PModalBottomSheet(
        modifier = Modifier.defaultMinSize(minHeight = 360.dp),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PBottomSheetTopAppBar(
                title = stringResource(Res.string.cast_select_screen),
                subtitle = stringResource(if (hasDevices) Res.string.cast_dialog_hint else Res.string.cast_looking_for_devices),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SearchingScreenAnimation(Modifier.fillMaxWidth().height(if (hasDevices) 104.dp else 156.dp))
            }
            if (hasDevices) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(devices, key = { it.hostAddress }) { device ->
                        CastDeviceCard(
                            title = device.getDeviceName(),
                            subtitle = device.hostAddress,
                            onClick = {
                                castVM.selectDevice(device.hostAddress)
                                audioPause()
                                scope.launch(Dispatchers.Default) {
                                    if (!ServicePreference.getAsync()) {
                                        ServicePreference.putAsync(true)
                                        sendEvent(StartHttpServerEvent())
                                    }
                                }
                                if (onDeviceSelected != null) {
                                    onDeviceSelected()
                                } else {
                                    castVM.enterCastMode()
                                }
                                onDismiss()
                            },
                        )
                    }
                }
            }
            Tips(stringResource(Res.string.cast_dialog_wireless_cast_tip))
            BottomSpace()
        }
    }
}
