package com.ismartcoding.plain.ui.page.scan

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.graphics.ImageFormat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavHostController
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.QrCodeBitmapHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.preference.ScanHistoryPreference
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.RequestPermissionsEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.QrCodeScanHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.TextCard
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.page.RouteName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPage(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val window = (view.context as Activity).window
    val insetsController = WindowCompat.getInsetsController(window, view)

    var cameraProvider: ProcessCameraProvider? = null
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }
    var cameraDetecting by remember { mutableStateOf(true) }
    var hasCamPermission by remember {
        mutableStateOf(Permission.CAMERA.can(context))
    }
    var showScanResultSheet by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        events.add(
            receiveEventHandler<PermissionsResultEvent> {
                hasCamPermission = Permission.CAMERA.can(context)
                if (!hasCamPermission) {
                    DialogHelper.showMessage(LocaleHelper.getString(R.string.scan_needs_camera_warning))
                }
            },
        )
        events.add(
            receiveEventHandler<PickFileResultEvent> { event ->
                if (event.tag != PickFileTag.SCAN) {
                    return@receiveEventHandler
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
            },
        )
    }
    if (!hasCamPermission) {
        sendEvent(RequestPermissionsEvent(Permission.CAMERA))
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            events.forEach { it.cancel() }
            events.clear()
            cameraProvider?.unbindAll()
        }
    }

    if (showScanResultSheet) {
        ScanResultBottomSheet(context, scanResult) {
            showScanResultSheet = false
            cameraDetecting = true
        }
    }

    PScaffold(
        navController,
        containerColor = Color.Transparent,
        navigationIcon = null,
        content = {
            Box(
                modifier = Modifier.fillMaxSize(),
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
                            preview.setSurfaceProvider(previewView.surfaceProvider)
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
                                val cameraProviderFeature = ProcessCameraProvider.getInstance(context)
                                cameraProvider = cameraProviderFeature.get()
                                cameraProvider?.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    imageAnalysis,
                                )
                            } catch (e: Exception) {
                                LogCat.e(e)
                                e.printStackTrace()
                            }
                            previewView
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                TopAppBar(
                    title = { },
                    modifier = Modifier.padding(top = 24.dp),
                    navigationIcon = {
                        PIconButton(
                            icon = Icons.Rounded.Cancel,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White,
                        ) {
                            navController.popBackStack()
                        }
                    },
                    colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 64.dp)
                        .align(Alignment.BottomCenter),
                ) {
                    PIconButton(
                        modifier = Modifier.size(40.dp),
                        containerModifier = Modifier.size(64.dp),
                        icon = Icons.Rounded.History,
                        contentDescription = stringResource(R.string.scan_history),
                        tint = Color.White,
                    ) {
                        navController.navigate(RouteName.SCAN_HISTORY)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    PIconButton(
                        modifier = Modifier.size(40.dp),
                        containerModifier = Modifier.size(size = 64.dp),
                        icon = Icons.Rounded.Image,
                        contentDescription = stringResource(R.string.images),
                        tint = Color.White,
                    ) {
                        sendEvent(PickFileEvent(PickFileTag.SCAN, PickFileType.IMAGE, multiple = false))
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanResultBottomSheet(
    context: Context,
    value: String,
    onDismiss: () -> Unit,
) {
    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        PBottomSheetTopAppBar(title = stringResource(id = R.string.scan_result)) {
            PIconButton(
                icon = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(android.R.string.copy),
                tint = MaterialTheme.colorScheme.onSurface,
            ) {
                val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.scan_result), value)
                clipboardManager.setPrimaryClip(clip)
                DialogHelper.showMessage(value)
            }
        }
        TopSpace()
        TextCard(context, text = value)
        BottomSpace()
    }
}
