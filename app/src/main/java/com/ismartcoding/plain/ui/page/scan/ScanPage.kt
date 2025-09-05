package com.ismartcoding.plain.ui.page.scan

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
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
import java.nio.ByteBuffer

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPage(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedFlow = Channel.sharedFlow

    var cameraProvider: ProcessCameraProvider? = null
    var cameraDetecting by remember { mutableStateOf(true) }
    var hasCamPermission by remember {
        mutableStateOf(Permission.CAMERA.can(context))
    }
    var showScanResultSheet by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf("") }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    hasCamPermission = Permission.CAMERA.can(context)
                    if (!hasCamPermission) {
                        DialogHelper.showMessage(LocaleHelper.getString(R.string.scan_needs_camera_warning))
                    }
                }

                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.SCAN) {
                        return@collect
                    }
                    coIO {
                        try {
                            cameraDetecting = false
                            DialogHelper.showLoading()
                            val originalImage = QrCodeBitmapHelper.getBitmapFromUri(context, event.uris.first())
                            val intArray = IntArray(originalImage.width * originalImage.height)
                            originalImage.getPixels(
                                intArray,
                                0,
                                originalImage.width,
                                0,
                                0,
                                originalImage.width,
                                originalImage.height
                            )

                            val result = QrCodeScanHelper.tryDecode(originalImage)
                            DialogHelper.hideLoading()
                            if (result != null) {
                                scanResult = result.text
                                addScanResult(context, scope, scanResult)
                                showScanResultSheet = true
                            }
                        } catch (ex: Exception) {
                            DialogHelper.hideLoading()
                            cameraDetecting = true
                            // the picked file could be deleted
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    if (!hasCamPermission) {
        sendEvent(RequestPermissionsEvent(Permission.CAMERA))
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    if (showScanResultSheet) {
        QrScanResultBottomSheet(context, scanResult) {
            showScanResultSheet = false
            cameraDetecting = true
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(id = R.string.scan_qrcode),
                actions = {
                    PIconButton(
                        icon = R.drawable.history,
                        contentDescription = stringResource(R.string.scan_history),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        navController.navigate(Routing.ScanHistory)
                    }
                },
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
            ) {
                if (hasCamPermission) {
                    AndroidView(
                        factory = { context ->
                            val previewView = PreviewView(context)
                            val preview = Preview.Builder().build()
                            val selector =
                                CameraSelector.Builder()
                                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                    .build()
                            preview.surfaceProvider = previewView.surfaceProvider
                            val imageAnalysis =
                                ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(context),
                                object : ImageAnalysis.Analyzer {
                                    private val supportedImageFormats =
                                        listOf(
                                            ImageFormat.YUV_420_888,
                                            ImageFormat.YUV_422_888,
                                            ImageFormat.YUV_444_888,
                                        )

                                    override fun analyze(imageProxy: ImageProxy) {
                                        if (cameraDetecting) {
                                            if (imageProxy.format in supportedImageFormats && imageProxy.planes.size == 3) {
                                                val data = imageProxy.planes[0].buffer.toByteArray()
                                                try {
                                                    cameraDetecting = false
                                                    val reader = QrCodeScanHelper.createReader()
                                                    try {
                                                        val result = decode(reader, imageProxy, data)
                                                        scanResult = result.text
                                                        addScanResult(context, scope, scanResult)
                                                        showScanResultSheet = true
                                                    } catch (e: NotFoundException) {
                                                        for (i in data.indices) data[i] = (255 - (data[i].toInt() and 0xff)).toByte()
                                                        val result = decode(reader, imageProxy, data)
                                                        scanResult = result.text
                                                        addScanResult(context, scope, scanResult)
                                                        showScanResultSheet = true
                                                    }
                                                } catch (e: java.lang.Exception) {
                                                    cameraDetecting = true
                                                    e.printStackTrace()
                                                } finally {
                                                    imageProxy.close()
                                                }
                                            }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }

                                    private fun ByteBuffer.toByteArray(): ByteArray {
                                        rewind()
                                        return ByteArray(remaining()).also {
                                            get(it)
                                        }
                                    }
                                },
                            )
                            try {
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                cameraProviderFuture.addListener({
                                    try {
                                        val provider = cameraProviderFuture.get()
                                        cameraProvider = provider
                                        provider.unbindAll()
                                        provider.bindToLifecycle(
                                            lifecycleOwner,
                                            selector,
                                            preview,
                                            imageAnalysis,
                                        )
                                    } catch (e: Exception) {
                                        LogCat.e(e)
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(context))
                            } catch (e: Exception) {
                                LogCat.e(e)
                                e.printStackTrace()
                            }
                            previewView
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 64.dp)
                            .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.darkMask(0.2f))
                            .clickable {
                                sendEvent(PickFileEvent(PickFileTag.SCAN, PickFileType.IMAGE, multiple = false))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.image),
                            contentDescription = stringResource(R.string.images),
                            tint = Color.White,
                        )
                    }
                }
            }
        },
    )
}

private fun decode(reader: MultiFormatReader, imageProxy: ImageProxy, data: ByteArray): Result {
    val source = PlanarYUVLuminanceSource(
        data,
        imageProxy.planes[0].rowStride,
        imageProxy.height,
        0,
        0,
        imageProxy.width,
        imageProxy.height,
        false
    )
    return reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
}

private fun addScanResult(
    context: Context,
    scope: CoroutineScope,
    value: String,
) {
    scope.launch {
        val results = withIO { ScanHistoryPreference.getValueAsync(context).toMutableList() }
        results.removeIf { it == value }
        results.add(0, value)
        withIO { ScanHistoryPreference.putAsync(context, results) }
    }
}

