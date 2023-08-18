package com.ismartcoding.plain.ui.page.scan

import android.content.Context
import android.graphics.ImageFormat
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.QrCodeBitmapHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.data.enums.PickFileType
import com.ismartcoding.plain.data.preference.ScanHistoryPreference
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionResultEvent
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.RequestPermissionEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.scan.ScanHistoryDialog
import com.ismartcoding.plain.ui.scan.ScanResultDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPage(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFeature = remember {
        ProcessCameraProvider.getInstance(context)
    }
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }
    val systemUiController = rememberSystemUiController()
    var cameraDetecting by remember { mutableStateOf(true) }

    var hasCamPermission by remember {
        mutableStateOf(Permission.CAMERA.can(context))
    }

    LaunchedEffect(Unit) {
        systemUiController.isSystemBarsVisible = false
        events.add(
            receiveEventHandler<PermissionResultEvent> {
                hasCamPermission = Permission.CAMERA.can(context)
                if (!hasCamPermission) {
                    DialogHelper.showMessage(LocaleHelper.getString(R.string.scan_needs_camera_warning))
                }
            })
        events.add(
            receiveEventHandler<PickFileResultEvent> { event ->
                if (event.tag != PickFileTag.SCAN) {
                    return@receiveEventHandler
                }
                coIO {
                    try {
                        cameraDetecting = false
                        DialogHelper.showLoading()
                        val bitmap = QrCodeBitmapHelper.getBitmapFromContentUri(context, event.uris.first())
                        val width = bitmap.width
                        val height = bitmap.height
                        val pixels = IntArray(width * height)
                        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                        val source = RGBLuminanceSource(width, height, pixels)
                        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                        val reader = QRCodeReader()
                        val result = reader.decode(binaryBitmap)
                        DialogHelper.hideLoading()
                        addScanResult(context, scope, result.text)
                        coMain {
                            ScanResultDialog(result.text) {
                                cameraDetecting = true
                            }.show()
                        }
                    } catch (ex: Exception) {
                        DialogHelper.hideLoading()
                        cameraDetecting = true
                        // the picked file could be deleted
                        ex.printStackTrace()
                    }
                }
            })
    }
    if (!hasCamPermission) {
        sendEvent(RequestPermissionEvent(Permission.CAMERA))
    }

    DisposableEffect(Unit) {
        onDispose {
            systemUiController.isSystemBarsVisible = true
            events.forEach { it.cancel() }
        }
    }

    PScaffold(
        navController,
        containerColor = Color.Transparent,
        navigationIcon = null,
        content = {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (hasCamPermission) {
                    AndroidView(
                        factory = { context ->
                            val previewView = PreviewView(context)
                            val preview = Preview.Builder().build()
                            val selector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(
                                    Size(
                                        previewView.width,
                                        previewView.height
                                    )
                                )
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(context),
                                object : ImageAnalysis.Analyzer {
                                    private val supportedImageFormats = listOf(
                                        ImageFormat.YUV_420_888,
                                        ImageFormat.YUV_422_888,
                                        ImageFormat.YUV_444_888
                                    )

                                    override fun analyze(image: ImageProxy) {
                                        if (cameraDetecting) {
                                            if (image.format in supportedImageFormats) {
                                                val bytes = image.planes.first().buffer.toByteArray()

                                                val source = PlanarYUVLuminanceSource(
                                                    bytes,
                                                    image.width,
                                                    image.height,
                                                    0,
                                                    0,
                                                    image.width,
                                                    image.height,
                                                    false
                                                )
                                                val binaryBmp = BinaryBitmap(HybridBinarizer(source))
                                                try {
                                                    cameraDetecting = false
                                                    val result = MultiFormatReader().apply {
                                                        setHints(
                                                            mapOf(
                                                                DecodeHintType.POSSIBLE_FORMATS to arrayListOf(
                                                                    BarcodeFormat.QR_CODE
                                                                )
                                                            )
                                                        )
                                                    }.decode(binaryBmp)
                                                    addScanResult(context, scope, result.text)
                                                    ScanResultDialog(result.text) {
                                                        cameraDetecting = true
                                                    }.show()
                                                } catch (e: java.lang.Exception) {
                                                    cameraDetecting = true
                                                    e.printStackTrace()
                                                } finally {
                                                    image.close()
                                                }
                                            }
                                        } else {
                                            image.close()
                                        }
                                    }

                                    private fun ByteBuffer.toByteArray(): ByteArray {
                                        rewind()
                                        return ByteArray(remaining()).also {
                                            get(it)
                                        }
                                    }

                                }
                            )
                            try {
                                cameraProviderFeature.get().bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                TopAppBar(
                    title = { },
                    modifier = Modifier.padding(top = 24.dp),
                    navigationIcon = {
                        PIconButton(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        ) {
                            navController.popBackStack()
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 64.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    PIconButton(
                        modifier = Modifier.size(40.dp),
                        containerModifier = Modifier.size(64.dp),
                        imageVector = Icons.Rounded.History,
                        contentDescription = stringResource(R.string.scan_history),
                        tint = Color.White
                    ) {
                        cameraDetecting = false
                        ScanHistoryDialog() {
                            cameraDetecting = true
                        }.show()
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    PIconButton(
                        modifier = Modifier.size(40.dp),
                        containerModifier = Modifier.size(size = 64.dp),
                        imageVector = Icons.Rounded.Image,
                        contentDescription = stringResource(R.string.images),
                        tint = Color.White
                    ) {
                        sendEvent(PickFileEvent(PickFileTag.SCAN, PickFileType.IMAGE, multiple = false))
                    }
                }
            }
        }
    )
}

private fun addScanResult(context: Context, scope: CoroutineScope, value: String) {
    scope.launch {
        val results = withIO { ScanHistoryPreference.getValueAsync(context).toMutableList() }
        results.remove(value)
        results.add(0, value)
        withIO { ScanHistoryPreference.putAsync(context, results) }
    }
}