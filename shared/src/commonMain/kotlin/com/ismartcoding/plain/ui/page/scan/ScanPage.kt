package com.ismartcoding.plain.ui.page.scan

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.discover.PairingInitiator
import com.ismartcoding.plain.discover.QrPairPayload
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.ScanCameraView
import com.ismartcoding.plain.platform.ScannedCode
import com.ismartcoding.plain.platform.ScannedFrame
import com.ismartcoding.plain.platform.ScannedImage
import com.ismartcoding.plain.platform.decodeQrFromUri
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.preferences.ScanHistoryPreference
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.QrScanResultBottomSheet
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.scan.components.ScanCodeTags
import com.ismartcoding.plain.ui.page.scan.components.ScanImageCodePicker
import com.ismartcoding.plain.ui.theme.darkMask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPage(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val cameraDetecting = remember { mutableStateOf(true) }
    var hasCamPermission by remember { mutableStateOf(Permission.CAMERA.isGranted()) }
    var showScanResultSheet by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf("") }
    // frozen multi-code frame: recognition stops until cancelled, so tags never jitter
    var frozenCodes by remember { mutableStateOf<List<ScannedCode>>(emptyList()) }
    var pickedImage by remember { mutableStateOf<ScannedImage?>(null) }
    var pickedImageUri by remember { mutableStateOf("") }
    var pairingInFlight by remember { mutableStateOf(false) }
    var handledText by remember { mutableStateOf<String?>(null) }
    val tracker = remember { ScanCodeTracker() }
    val autoOpenPolicy = remember { ScanAutoOpenPolicy() }
    val freezeFrame = remember { mutableStateOf<ScannedFrame?>(null) }

    val multiFrozen = frozenCodes.size >= 2
    val showingPicker = pickedImage != null
    val frozenSnapshot = freezeFrame.value

    fun resumeScanning() {
        frozenCodes = emptyList()
        freezeFrame.value = null
        tracker.reset()
        autoOpenPolicy.reset()
        handledText = null
        cameraDetecting.value = true
    }

    fun resumeIfIdle() {
        if (frozenCodes.isEmpty() && pickedImage == null && !showScanResultSheet) {
            freezeFrame.value = null
            tracker.reset()
            autoOpenPolicy.reset()
            handledText = null
            cameraDetecting.value = true
        }
    }

    fun openResult(text: String) {
        scanResult = text
        addScanResult(scope, text)
        cameraDetecting.value = false
        showScanResultSheet = true
    }

    fun startQrPairing(payload: QrPairPayload) {
        pairingInFlight = true
        cameraDetecting.value = false
        scope.launch {
            val title = LocaleHelper.getStringFAsync(Res.string.pair_with_device, payload.name)
            val message = LocaleHelper.getStringFAsync(Res.string.confirm_pair_with_device, payload.name)
            DialogHelper.showConfirmDialog(
                title = title,
                message = message,
                confirmButton = Pair(LocaleHelper.getStringAsync(Res.string.confirm)) {
                    pairingInFlight = false
                    resumeIfIdle()
                    coIO {
                        PairingInitiator.start(payload.toDevice())
                        DialogHelper.showSuccess(Res.string.qr_pair_request_sent)
                    }
                },
                dismissButton = Pair(LocaleHelper.getStringAsync(Res.string.cancel)) {
                    pairingInFlight = false
                    resumeIfIdle()
                },
            )
        }
    }

    fun handleScanResult(text: String) {
        val qrPairPayload = QrPairPayload.parse(text)
        if (qrPairPayload != null) {
            startQrPairing(qrPairPayload)
            return
        }
        openResult(text)
    }

    /**
     * Per-frame handler: a code only surfaces after [ScanCodeTracker] confirms it on
     * consecutive frames. Several codes freeze recognition (WeChat-style) with tappable
     * tags; a single code auto-opens only while no other unconfirmed code shares the
     * frame (see [ScanAutoOpenPolicy]), so a multi-code scene never pops a sheet by
     * itself.
     */
    fun onFrameCodes(codes: List<ScannedCode>) {
        if (frozenCodes.isNotEmpty()) return
        val confirmed = tracker.accept(codes)
        if (handledText != null && confirmed.none { it.text == handledText }) {
            handledText = null
        }
        if (confirmed.size >= 2) {
            frozenCodes = confirmed
            cameraDetecting.value = false
            return
        }
        if (confirmed.size == 1) {
            val code = confirmed.first()
            if (autoOpenPolicy.allow(confirmed.size, codes.size) &&
                handledText == null && !showScanResultSheet && !pairingInFlight && pickedImage == null
            ) {
                handledText = code.text
                cameraDetecting.value = false
                handleScanResult(code.text)
            }
        }
    }

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    hasCamPermission = Permission.CAMERA.isGranted(); if (!hasCamPermission) DialogHelper.showMessage(LocaleHelper.getStringAsync(Res.string.scan_needs_camera_warning))
                }

                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.SCAN) return@collect
                    coIO {
                        try {
                            cameraDetecting.value = false; DialogHelper.showLoading()
                            val result = decodeQrFromUri(event.uris.first())
                            DialogHelper.hideLoading()
                            when {
                                result == null -> {
                                    DialogHelper.showMessage(LocaleHelper.getStringAsync(Res.string.scan_no_code_found))
                                    resumeIfIdle()
                                }

                                result.codes.size == 1 -> handleScanResult(result.codes.first().text)

                                else -> {
                                    pickedImageUri = event.uris.first()
                                    pickedImage = result
                                }
                            }
                        } catch (ex: Exception) {
                            DialogHelper.hideLoading(); resumeIfIdle(); ex.printStackTrace()
                        }
                    }
                }
            }
        }
    }
    if (!hasCamPermission) sendEvent(RequestPermissionsEvent(Permission.CAMERA))
    if (showScanResultSheet) {
        QrScanResultBottomSheet(scanResult) {
            showScanResultSheet = false
            resumeIfIdle()
        }
    }

    PScaffold(topBar = {
        PTopAppBar(
            navController = navController,
            navigationIcon = if (multiFrozen || showingPicker) {
                {
                    NavigationCloseIcon {
                        if (showingPicker) {
                            pickedImage = null
                            resumeIfIdle()
                        } else {
                            resumeScanning()
                        }
                    }
                }
            } else {
                null
            },
            title = stringResource(Res.string.scan_qrcode),
            actions = {
                PIconButton(
                    icon = Res.drawable.history,
                    contentDescription = stringResource(Res.string.scan_history),
                    tint = MaterialTheme.colorScheme.onSurface
                ) { navController.navigate(Routing.ScanHistory) }
            })
    }, content = { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (hasCamPermission) {
                ScanCameraView(cameraDetecting, freezeFrame, onScanResult = { onFrameCodes(it) })
                // freeze the detected frame over the live preview (WeChat-style);
                // tag positions come from the snapshot so they match the image
                if (frozenSnapshot != null && !showingPicker) {
                    Image(
                        bitmap = frozenSnapshot.bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                val tagCodes = frozenSnapshot?.codes?.takeIf { it.size >= 2 } ?: frozenCodes.takeIf { multiFrozen }
                if (tagCodes != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )
                    ScanCodeTags(codes = tagCodes) { code -> handleScanResult(code.text) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 144.dp)
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.scan_multiple_codes_hint),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.darkMask(0.6f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 64.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.darkMask(0.2f))
                        .clickable { sendEvent(PickFileEvent(PickFileTag.SCAN, PickFileType.IMAGE, multiple = false)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painter = painterResource(Res.drawable.image), contentDescription = stringResource(Res.string.images), tint = Color.White)
                }
            }
            pickedImage?.let { image ->
                ScanImageCodePicker(
                    uri = pickedImageUri,
                    image = image,
                    // the picker stays up so the user can open another tag after
                    // dismissing the result sheet; only the X button closes it
                    onPick = { code -> handleScanResult(code.text) },
                )
            }
        }
    })
}

private fun addScanResult(scope: CoroutineScope, value: String) {
    scope.launch {
        val results = ScanHistoryPreference.getValueAsync().toMutableList()
        results.removeAll { it == value }
        results.add(0, value)
        ScanHistoryPreference.putAsync(results)
    }
}
