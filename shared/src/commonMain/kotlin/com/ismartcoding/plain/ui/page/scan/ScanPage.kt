package com.ismartcoding.plain.ui.page.scan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ismartcoding.plain.discover.PairingInitiator
import com.ismartcoding.plain.discover.QrPairPayload
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.preferences.UserPrefs
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.QrScanResultBottomSheet
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.scanner.QrCodeScanner
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.history as ui_drawable_history
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image
import com.ismartcoding.plain.ui.resources.close as ui_drawable_close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPage(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var cameraPermissionGranted by remember { mutableStateOf(Permission.CAMERA.isGranted()) }
    var onImagePicked by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var showScanResultSheet by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf("") }
    var scannerHasCloseAction by remember { mutableStateOf(false) }
    var closeScannerRequest by remember { mutableStateOf(0) }
    var resumeScannerAfterResult by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    cameraPermissionGranted = Permission.CAMERA.isGranted()
                    if (!cameraPermissionGranted) {
                        DialogHelper.showMessage(LocaleHelper.getStringAsync(Res.string.scan_needs_camera_warning))
                    }
                }

                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.SCAN) return@collect
                    val callback = onImagePicked
                    onImagePicked = null
                    event.uris.firstOrNull()?.let { callback?.invoke(it) }
                }
            }
        }
    }

    LaunchedEffect(cameraPermissionGranted) {
        if (!cameraPermissionGranted) sendEvent(RequestPermissionsEvent(Permission.CAMERA))
    }

    PScaffold(topBar = {
        PTopAppBar(
            onNavigateBack = { navController.navigateUp() },
            navigationIcon = if (scannerHasCloseAction) {
                {
                    IconButton(
                        onClick = { closeScannerRequest += 1 }
                    ) {
                        Icon(
                            painter = painterResource(UiRes.drawable.ui_drawable_close),
                            contentDescription = stringResource(Res.string.close),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else {
                null
            },
            title = stringResource(Res.string.scan_qrcode),
            actions = {
                IconButton(onClick = { navController.navigate(Routing.ScanHistory) }) {
                    Icon(
                        painter = painterResource(UiRes.drawable.ui_drawable_history),
                        contentDescription = stringResource(Res.string.scan_history),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            })
    }, content = { paddingValues ->
        QrCodeScanner(
            cameraPermissionGranted = cameraPermissionGranted,
            multipleCodesHint = stringResource(Res.string.scan_multiple_codes_hint),
            imagePickerDescription = stringResource(Res.string.images),
            closeRequest = closeScannerRequest,
            onCloseActionVisibilityChanged = { scannerHasCloseAction = it },
            pickImage = { callback ->
                onImagePicked = callback
                sendEvent(PickFileEvent(PickFileTag.SCAN, PickFileType.IMAGE, multiple = false))
            },
            onScanResult = { value, onDismiss ->
                scope.launch {
                    val results = UserPrefs.scanHistoryValue().toMutableList()
                    results.removeAll { it == value }
                    results.add(0, value)
                    UserPrefs.setScanHistory(results)
                }
                scanResult = value
                resumeScannerAfterResult = onDismiss
                showScanResultSheet = true
            },
            handleSpecialCode = { text, onFinished ->
                val payload = QrPairPayload.parse(text)
                if (payload == null) {
                    false
                } else {
                    scope.launch {
                        val title = LocaleHelper.getStringFAsync(Res.string.pair_with_device, payload.name)
                        val message = LocaleHelper.getStringFAsync(Res.string.confirm_pair_with_device, payload.name)
                        DialogHelper.showConfirmDialog(
                            title = title,
                            message = message,
                            confirmButton = Pair(LocaleHelper.getStringAsync(Res.string.confirm)) {
                                onFinished()
                                coIO {
                                    PairingInitiator.start(payload.toDevice())
                                    DialogHelper.showSuccess(Res.string.qr_pair_request_sent)
                                }
                            },
                            dismissButton = Pair(LocaleHelper.getStringAsync(Res.string.cancel), onFinished),
                        )
                    }
                    true
                }
            },
            showNoCodeFound = {
                scope.launch {
                    DialogHelper.showMessage(LocaleHelper.getStringAsync(Res.string.scan_no_code_found))
                }
            },
            showImageLoading = { DialogHelper.showLoading() },
            hideImageLoading = { DialogHelper.hideLoading() },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        )
    })

    if (showScanResultSheet) {
        QrScanResultBottomSheet(
            scanResult,
            title = stringResource(Res.string.scan_result),
            copyLabel = stringResource(Res.string.scan_result),
        ) {
            showScanResultSheet = false
            resumeScannerAfterResult?.invoke()
            resumeScannerAfterResult = null
        }
    }
}