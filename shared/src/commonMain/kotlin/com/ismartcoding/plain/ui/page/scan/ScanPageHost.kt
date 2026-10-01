package com.ismartcoding.plain.ui.page.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.scanner.ScanPage
import com.ismartcoding.plain.ui.scanner.ScanPageActions
import com.ismartcoding.plain.ui.scanner.ScanPageTexts
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.i18n.*

@Composable
fun ScanPageHost(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var cameraPermissionGranted by remember { mutableStateOf(Permission.CAMERA.isGranted()) }
    var onImagePicked by remember { mutableStateOf<((String) -> Unit)?>(null) }

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

    ScanPage(
        cameraPermissionGranted = cameraPermissionGranted,
        texts = ScanPageTexts(
            scanTitle = stringResource(Res.string.scan_qrcode),
            scanHistory = stringResource(Res.string.scan_history),
            close = stringResource(Res.string.close),
            multipleCodesHint = stringResource(Res.string.scan_multiple_codes_hint),
            imagePickerDescription = stringResource(Res.string.images),
            scanResultTitle = stringResource(Res.string.scan_result),
            scanResultCopyLabel = stringResource(Res.string.scan_result),
        ),
        actions = ScanPageActions(
            requestCameraPermission = { sendEvent(RequestPermissionsEvent(Permission.CAMERA)) },
            navigateBack = { navController.navigateUp() },
            openHistory = { navController.navigate(Routing.ScanHistory) },
            pickImage = { callback ->
                onImagePicked = callback
                sendEvent(PickFileEvent(PickFileTag.SCAN, PickFileType.IMAGE, multiple = false))
            },
            recordScanResult = { value ->
                scope.launch {
                    val results = UserPrefs.scanHistoryValue().toMutableList()
                    results.removeAll { it == value }
                    results.add(0, value)
                    UserPrefs.setScanHistory(results)
                }
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
        ),
    )
}