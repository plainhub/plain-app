package com.ismartcoding.plain.lib.extensions

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.telecom.TelecomManager
import android.view.WindowManager
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import java.io.File

fun Context.dp2px(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

fun Context.hasPermission(vararg permission: String): Boolean {
    return permission.toSet().all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
}

val Context.notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

fun Context.scanFileByConnection(
    file: File,
    callback: MediaScannerConnection.OnScanCompletedListener? = null,
) {
    val path = file.absolutePath
    val mimeType = file.name.getMimeType()
    scanFileByConnection(arrayOf(path), arrayOf(mimeType), callback)
}

fun Context.scanFileByConnection(
    path: String,
    callback: MediaScannerConnection.OnScanCompletedListener? = null,
) {
    val mimeType = path.getMimeType()
    scanFileByConnection(arrayOf(path), arrayOf(mimeType), callback)
}

fun Context.scanFileByConnection(
    paths: Array<String>,
    mimeTypes: Array<String>? = null,
    callback: MediaScannerConnection.OnScanCompletedListener? = null,
) {
    MediaScannerConnection.scanFile(this, paths, mimeTypes, callback)
}

fun Context.getMimeTypeFromUri(uri: Uri): String {
    var mimetype = uri.path?.getMimeType() ?: ""
    if (mimetype.isEmpty()) {
        try {
            mimetype = contentResolver.getType(uri) ?: ""
        } catch (e: IllegalStateException) {
        }
    }
    return mimetype
}

fun Context.isGestureInteractionMode(): Boolean {
    val resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
    if (resourceId == 0) {
        return false
    }
    return resources.getInteger(resourceId) == 2
}

