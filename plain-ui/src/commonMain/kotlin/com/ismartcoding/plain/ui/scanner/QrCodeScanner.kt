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
import com.ismartcoding.plain.ui.scanner.components.ScanCodeTags
import com.ismartcoding.plain.ui.scanner.components.ScanImageCodePicker
import com.ismartcoding.plain.ui.theme.darkMask
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image

@Composable
fun QrCodeScanner(
    cameraPermissionGranted: Boolean,
    multipleCodesHint: String,
    imagePickerDescription: String,
    closeRequest: Int,
    onCloseActionVisibilityChanged: (Boolean) -> Unit,
    pickImage: ((String) -> Unit) -> Unit,
    onScanResult: (String, () -> Unit) -> Unit,
    handleSpecialCode: (String, () -> Unit) -> Boolean,
    showNoCodeFound: () -> Unit,
    showImageLoading: () -> Unit,
    hideImageLoading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val decodeQrImage = rememberQrImageDecoder()
    val cameraDetecting = remember { mutableStateOf(true) }
    var resultPending by remember { mutableStateOf(false) }
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
        if (frozenCodes.isEmpty() && pickedImage == null && !resultPending) {
            freezeFrame.value = null
            tracker.reset()
            autoOpenPolicy.reset()
            handledText = null
            cameraDetecting.value = true
        }
    }

    fun openResult(text: String) {
        resultPending = true
        onScanResult(text) {
            resultPending = false
            resumeIfIdle()
        }
    }

    fun handleScanResult(text: String) {
        pairingInFlight = true
        cameraDetecting.value = false
        val handled = handleSpecialCode(text) {
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
     * consecutive frames. Several codes freeze recognition with tappable tags; a single
     * code auto-opens only while no other unconfirmed code shares the frame.
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
                handledText == null && !resultPending && !pairingInFlight && pickedImage == null
            ) {
                handledText = code.text
                cameraDetecting.value = false
                handleScanResult(code.text)
            }
        }
    }

    LaunchedEffect(multiFrozen, showingPicker) {
        onCloseActionVisibilityChanged(multiFrozen || showingPicker)
    }

    LaunchedEffect(closeRequest) {
        if (closeRequest > 0) {
            if (showingPicker) {
                pickedImage = null
                resumeIfIdle()
            } else {
                resumeScanning()
            }
        }
    }

    Box(modifier = modifier) {
        if (cameraPermissionGranted) {
            ScanCameraView(cameraDetecting, freezeFrame, onScanResult = { onFrameCodes(it) })
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
                        text = multipleCodesHint,
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
                        pickImage { uri ->
                            scope.launch {
                                cameraDetecting.value = false
                                showImageLoading()
                                try {
                                    val result = decodeQrImage(uri)
                                    when {
                                        result == null -> {
                                            showNoCodeFound()
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
                                    hideImageLoading()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(UiRes.drawable.ui_drawable_image), contentDescription = imagePickerDescription, tint = Color.White)
            }
        }
        pickedImage?.let { image ->
            ScanImageCodePicker(
                uri = pickedImageUri,
                image = image,
                onPick = { code -> handleScanResult(code.text) },
            )
        }
    }
}