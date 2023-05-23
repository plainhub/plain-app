package com.ismartcoding.plain.ui.scan

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.google.zxing.Result
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.lib.helpers.BitmapHelper
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.data.enums.PickFileType
import com.ismartcoding.plain.databinding.DialogScanBinding
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.king.zxing.CameraScan
import com.king.zxing.CameraScan.OnScanResultCallback
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.DefaultCameraScan
import com.king.zxing.analyze.MultiFormatAnalyzer
import com.king.zxing.config.ResolutionCameraConfig
import com.king.zxing.util.CodeUtils

class ScanDialog() : BaseDialog<DialogScanBinding>(), OnScanResultCallback {
    private var mCameraScan: CameraScan? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity(), R.style.Theme_FullScreen).apply {
            window?.fullScreen()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.onBack {
            dismiss()
        }

        receiveEvent<PermissionResultEvent> { event ->
            if (Permission.CAMERA.can()) {
                startCamera()
            } else {
                DialogHelper.showMessage(getString(R.string.scan_needs_camera_warning))
            }
        }

        receiveEvent<PickFileResultEvent> { event ->
            if (event.tag != PickFileTag.SCAN) {
                return@receiveEvent
            }
            coIO {
                try {
                    DialogHelper.showLoading()
                    val bitmap = BitmapHelper.getBitmapFromContentUri(requireContext(), event.uris.first())
                    // TODO: OOM needs to be fixed
                    val result = CodeUtils.parseQRCode(bitmap)
                    DialogHelper.hideLoading()
                    addScanResult(result)
                    coMain {
                        ScanResultDialog(result) {
                            startCamera()
                        }.show()
                    }
                } catch (ex: Exception) {
                    // the picked file could be deleted
                    ex.printStackTrace()
                }
            }
        }

        binding.history.setSafeClick {
            mCameraScan = null
            ScanHistoryDialog() {
                startCamera()
            }.show()
        }

        binding.photos.setSafeClick {
            sendEvent(PickFileEvent(PickFileTag.SCAN, PickFileType.IMAGE, multiple = false))
        }

        if (Permission.CAMERA.can()) {
            startCamera()
        } else {
            Permission.CAMERA.grant()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraScan?.release()
    }

    private fun startCamera() {
        if (isActive && mCameraScan == null) { // if dialog is dismissed, should not start camera
            //初始化解码配置
            val decodeConfig = DecodeConfig()
            decodeConfig.setHints(DecodeFormatManager.QR_CODE_HINTS).isFullAreaScan = true //设置是否全区域识别，默认false

            mCameraScan = DefaultCameraScan(this, binding.previewView)
            mCameraScan?.setOnScanResultCallback(this)
                ?.setAnalyzer(MultiFormatAnalyzer(decodeConfig))
                ?.setVibrate(true)
                ?.setCameraConfig(ResolutionCameraConfig(requireContext(), ResolutionCameraConfig.IMAGE_QUALITY_720P))
                ?.startCamera()
        }
    }

    private fun addScanResult(value: String) {
        val results = LocalStorage.scanResults.toMutableList()
        results.remove(value)
        results.add(0, value)
        LocalStorage.scanResults = results
    }

    override fun onScanResultCallback(result: Result): Boolean {
        mCameraScan?.release()
        mCameraScan = null
        addScanResult(result.text)
        ScanResultDialog(result.text) {
            startCamera()
        }.show()
        return true
    }
}