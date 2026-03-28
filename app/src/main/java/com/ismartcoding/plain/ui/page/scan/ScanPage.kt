package com.ismartcoding.plain.ui.page.scan

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DQrPairData
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.QrCodeBitmapHelper
import com.ismartcoding.plain.helpers.QrCodeScanHelper
import com.ismartcoding.plain.preferences.ScanHistoryPreference
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.QrScanResultBottomSheet
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.darkMask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPage(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraProvider: ProcessCameraProvider? = null
    val cameraDetecting = remember { mutableStateOf(true) }
    var hasCamPermission by remember { mutableStateOf(Permission.CAMERA.can(context)) }
    var showScanResultSheet by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf("") }
    var pendingPairData by remember { mutableStateOf<DQrPairData?>(null) }

    fun handleScanResult(text: String) {
        scanResult = text; addScanResult(context, scope, text)
        val pairData = DQrPairData.fromQrContent(text)
        if (pairData != null) pendingPairData = pairData else showScanResultSheet = true
    }

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> { hasCamPermission = Permission.CAMERA.can(context); if (!hasCamPermission) DialogHelper.showMessage(LocaleHelper.getString(R.string.scan_needs_camera_warning)) }
                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.SCAN) return@collect
                    coIO {
                        try {
                            cameraDetecting.value = false; DialogHelper.showLoading()
                            val img = QrCodeBitmapHelper.getBitmapFromUri(context, event.uris.first())
                            val result = QrCodeScanHelper.tryDecode(img)
                            DialogHelper.hideLoading()
                            if (result != null) handleScanResult(result.text)
                        } catch (ex: Exception) { DialogHelper.hideLoading(); cameraDetecting.value = true; ex.printStackTrace() }
                    }
                }
            }
        }
    }
    if (!hasCamPermission) sendEvent(RequestPermissionsEvent(Permission.CAMERA))
    DisposableEffect(Unit) { onDispose { cameraProvider?.unbindAll() } }
    if (showScanResultSheet) { QrScanResultBottomSheet(context, scanResult) { showScanResultSheet = false; cameraDetecting.value = true } }
    pendingPairData?.let { pairData ->
        AlertDialog(onDismissRequest = { pendingPairData = null; cameraDetecting.value = true },
            title = { Text(stringResource(R.string.pair_via_qr_title)) },
            text = { Text(stringResource(R.string.confirm_pair_with_device, pairData.name)) },
            confirmButton = { Button(onClick = { navController.navigate(Routing.Nearby(JsonHelper.jsonEncode(pairData))) { popUpTo(Routing.Scan) { inclusive = true } }; pendingPairData = null }) { Text(stringResource(R.string.pair)) } },
            dismissButton = { TextButton(onClick = { pendingPairData = null; cameraDetecting.value = true }) { Text(stringResource(R.string.cancel)) } })
    }

    PScaffold(topBar = {
        PTopAppBar(navController = navController, title = stringResource(id = R.string.scan_qrcode), actions = {
            PIconButton(icon = R.drawable.history, contentDescription = stringResource(R.string.scan_history), tint = MaterialTheme.colorScheme.onSurface) { navController.navigate(Routing.ScanHistory) }
        })
    }, content = { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
            if (hasCamPermission) ScanCameraView(lifecycleOwner, cameraDetecting, onCameraProvider = { cameraProvider = it }, onScanResult = { handleScanResult(it) })
            Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 64.dp).align(Alignment.BottomCenter), horizontalArrangement = Arrangement.End) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.darkMask(0.2f)).clickable { sendEvent(PickFileEvent(PickFileTag.SCAN, PickFileType.IMAGE, multiple = false)) }, contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.image), contentDescription = stringResource(R.string.images), tint = Color.White)
                }
            }
        }
    })
}

private fun addScanResult(context: Context, scope: CoroutineScope, value: String) {
    scope.launch {
        val results = withIO { ScanHistoryPreference.getValueAsync(context).toMutableList() }
        results.removeIf { it == value }; results.add(0, value)
        withIO { ScanHistoryPreference.putAsync(context, results) }
    }
}
