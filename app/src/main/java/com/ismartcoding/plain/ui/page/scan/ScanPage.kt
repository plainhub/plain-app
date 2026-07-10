package com.ismartcoding.plain.ui.page.scan

import com.ismartcoding.plain.i18n.*
import android.annotation.SuppressLint
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.channel.Channel
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.extensions.isGestureInteractionMode
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.data.DQrPairData
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
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

    fun handleScanResult(text: String) {
        scanResult = text
        addScanResult(scope, text)
        val pairData = DQrPairData.fromQrContent(text)
        if (pairData != null) {
            sendEvent(PairingRequestReceivedEvent(pairData.toDPairingRequest()))
            cameraDetecting.value = false
        } else {
            showScanResultSheet = true
        }
    }

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    hasCamPermission = Permission.CAMERA.can(context); if (!hasCamPermission) DialogHelper.showMessage(LocaleHelper.getStringAsync(Res.string.scan_needs_camera_warning))
                }

                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.SCAN) return@collect
                    coIO {
                        try {
                            cameraDetecting.value = false; DialogHelper.showLoading()
                            val img = QrCodeBitmapHelper.getBitmapFromUri(context, event.uris.first())
                            val result = QrCodeScanHelper.tryDecode(img)
                            DialogHelper.hideLoading()
                            if (result != null) handleScanResult(result.text)
                        } catch (ex: Exception) {
                            DialogHelper.hideLoading(); cameraDetecting.value = true; ex.printStackTrace()
                        }
                    }
                }
            }
        }
    }
    if (!hasCamPermission) sendEvent(RequestPermissionsEvent(Permission.CAMERA))
    DisposableEffect(Unit) { onDispose { cameraProvider?.unbindAll() } }
    if (showScanResultSheet) {
        QrScanResultBottomSheet(scanResult) { showScanResultSheet = false; cameraDetecting.value = true }
    }

    PScaffold(topBar = {
        PTopAppBar(navController = navController, title = stringResource(Res.string.scan_qrcode), actions = {
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
            if (hasCamPermission) ScanCameraView(lifecycleOwner, cameraDetecting, onCameraProvider = { cameraProvider = it }, onScanResult = { handleScanResult(it) })
            if (hasCamPermission) ScanOverlay()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 64.dp)
                    .align(Alignment.BottomCenter), horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.darkMask(0.2f))
                        .clickable { sendEvent(PickFileEvent(PickFileTag.SCAN, PickFileType.IMAGE, multiple = false)) }, contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(Res.drawable.image), contentDescription = stringResource(Res.string.images), tint = Color.White)
                }
            }
        }
    })
}

private fun addScanResult(scope: CoroutineScope, value: String) {
    scope.launch {
        val results = ScanHistoryPreference.getValueAsync().toMutableList()
        results.removeIf { it == value }; results.add(0, value)
        ScanHistoryPreference.putAsync(results)
    }
}

@Composable
private fun ScanOverlay(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bottomInset = if (context.isGestureInteractionMode()) {
        0.dp
    } else {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan_line",
    )
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = bottomInset)
    ) {
        val boxSize = minOf(size.width, size.height) * 0.65f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f

        // Dark overlay around scan area
        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, 0f), size = Size(size.width, top))
        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, top + boxSize), size = Size(size.width, size.height - top - boxSize))
        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, top), size = Size(left, boxSize))
        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(left + boxSize, top), size = Size(size.width - left - boxSize, boxSize))

        // Corner decorations
        val cornerLen = 40.dp.toPx()
        val cornerStroke = 3.dp.toPx()
        val white = Color.White
        // top-left
        drawLine(white, Offset(left, top), Offset(left + cornerLen, top), cornerStroke)
        drawLine(white, Offset(left, top), Offset(left, top + cornerLen), cornerStroke)
        // top-right
        drawLine(white, Offset(left + boxSize, top), Offset(left + boxSize - cornerLen, top), cornerStroke)
        drawLine(white, Offset(left + boxSize, top), Offset(left + boxSize, top + cornerLen), cornerStroke)
        // bottom-left
        drawLine(white, Offset(left, top + boxSize), Offset(left + cornerLen, top + boxSize), cornerStroke)
        drawLine(white, Offset(left, top + boxSize), Offset(left, top + boxSize - cornerLen), cornerStroke)
        // bottom-right
        drawLine(white, Offset(left + boxSize, top + boxSize), Offset(left + boxSize - cornerLen, top + boxSize), cornerStroke)
        drawLine(white, Offset(left + boxSize, top + boxSize), Offset(left + boxSize, top + boxSize - cornerLen), cornerStroke)

        // Animated scan line
        val lineY = top + boxSize * scanProgress
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF00E676).copy(alpha = 0.9f),
                    Color(0xFF00E676),
                    Color(0xFF00E676).copy(alpha = 0.9f),
                    Color.Transparent,
                ),
                startX = left,
                endX = left + boxSize,
            ),
            topLeft = Offset(left, lineY - 1.5.dp.toPx()),
            size = Size(boxSize, 3.dp.toPx()),
        )
    }
}
