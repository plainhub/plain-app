package com.ismartcoding.plain.ui.platform.media

import android.graphics.BitmapFactory
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.ismartcoding.plain.ui.media.SvgHelper
import com.ismartcoding.plain.ui.scanner.QrCodeScanHelper

actual fun tryDecodeQrCode(path: String): String? {
    return try {
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        QrCodeScanHelper.tryDecode(bitmap)
    } catch (e: Exception) {
        null
    }
}

actual fun getSvgSize(path: String): IntSize = SvgHelper.getSize(path)

internal actual fun tryDecodeQrCode(image: ImageBitmap): String? = QrCodeScanHelper.tryDecode(image.asAndroidBitmap())
