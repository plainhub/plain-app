package com.ismartcoding.plain.ui.scanner

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
import androidx.compose.material3.IconButton
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
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.scanner.components.QrScanResultBottomSheet
import com.ismartcoding.plain.ui.scanner.components.ScanCodeTags
import com.ismartcoding.plain.ui.scanner.components.ScanImageCodePicker
import com.ismartcoding.plain.ui.theme.darkMask
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.history as ui_drawable_history
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image
import com.ismartcoding.plain.ui.resources.close as ui_drawable_close

data class ScanPageTexts(
    val scanTitle: String,
    val scanHistory: String,
    val close: String,
    val multipleCodesHint: String,
    val imagePickerDescription: String,
    val scanResultTitle: String,
    val scanResultCopyLabel: String,
)

data class ScanPageActions(
    val requestCameraPermission: () -> Unit,
    val navigateBack: () -> Unit,
    val openHistory: () -> Unit,
    val pickImage: ((String) -> Unit) -> Unit,
    val recordScanResult: (String) -> Unit,
    val handleSpecialCode: (String, () -> Unit) -> Boolean,
    val showNoCodeFound: () -> Unit,
    val showImageLoading: () -> Unit,
    val hideImageLoading: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPage(
    cameraPermissionGranted: Boolean,
    texts: ScanPageTexts,
    actions: ScanPageActions,
) {
    val scope = rememberCoroutineScope()
    val decodeQrImage = rememberQrImageDecoder()
    val cameraDetecting = remember { mutableStateOf(true) }
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
        actions.recordScanResult(text)
        cameraDetecting.value = false
        showScanResultSheet = true
    }

    fun handleScanResult(text: String) {
        pairingInFlight = true
        cameraDetecting.value = false
        val handled = actions.handleSpecialCode(text) {
            pairingInFlight = false
            resumeIfIdle()
        }
        if (!handled) {
            pairingInFlight = false
            openResult(text)
        }
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

    LaunchedEffect(cameraPermissionGranted) {
        if (!cameraPermissionGranted) actions.requestCameraPermission()
    }

    if (showScanResultSheet) {
        QrScanResultBottomSheet(scanResult, texts.scanResultTitle, texts.scanResultCopyLabel) {
            showScanResultSheet = false
            resumeIfIdle()
        }
    }

    PScaffold(topBar = {
        PTopAppBar(
            onNavigateBack = actions.navigateBack,
            navigationIcon = if (multiFrozen || showingPicker) {
                {
                    IconButton(
                        onClick = {
                            if (showingPicker) {
                                pickedImage = null
                                resumeIfIdle()
                            } else {
                                resumeScanning()
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(UiRes.drawable.ui_drawable_close),
                            contentDescription = texts.close,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else {
                null
            },
            title = texts.scanTitle,
            actions = {
                IconButton(onClick = actions.openHistory) {
                    Icon(
                        painter = painterResource(UiRes.drawable.ui_drawable_history),
                        contentDescription = texts.scanHistory,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            })
    }, content = { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (cameraPermissionGranted) {
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
                            text = texts.multipleCodesHint,
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
                        .clickable {
                            actions.pickImage { uri ->
                                scope.launch {
                                    cameraDetecting.value = false
                                    actions.showImageLoading()
                                    try {
                                        val result = decodeQrImage(uri)
                                        when {
                                            result == null -> {
                                                actions.showNoCodeFound()
                                                resumeIfIdle()
                                            }

                                            result.codes.size == 1 -> handleScanResult(result.codes.first().text)

                                            else -> {
                                                pickedImageUri = uri
                                                pickedImage = result
                                            }
                                        }
                                    } catch (e: Exception) {
                                        resumeIfIdle()
                                    } finally {
                                        actions.hideImageLoading()
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painter = painterResource(UiRes.drawable.ui_drawable_image), contentDescription = texts.imagePickerDescription, tint = Color.White)
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
