package com.ismartcoding.lib.extensions

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

fun Context.px(
    @DimenRes dimen: Int,
): Int = resources.getDimension(dimen).toInt()

fun Context.dp(
    @DimenRes dimen: Int,
): Float = resources.getDimensionPixelSize(dimen) / resources.displayMetrics.density

fun Context.dp2px(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

fun Context.hasPermission(vararg permission: String): Boolean {
    return permission.toSet().all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
}

val Context.telecomManager: TelecomManager get() = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
val Context.notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
val Context.shortcutManager: ShortcutManager get() = getSystemService(ShortcutManager::class.java) as ShortcutManager

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

fun <T> Context.getSystemServiceCompat(serviceClass: Class<T>): T = ContextCompat.getSystemService(this, serviceClass)!!

fun Context.getCompatDrawable(
    @DrawableRes drawableId: Int,
) = ContextCompat.getDrawable(this, drawableId)

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

fun Context.isWifiConnected(): Boolean {
    val cm = getSystemServiceCompat(ConnectivityManager::class.java)
    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
    return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
}

fun Context.isGestureInteractionMode(): Boolean {
    val resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
    if (resourceId == 0) {
        return false
    }
    return resources.getInteger(resourceId) == 2
}

fun Context.isPortrait(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}

fun Context.isTV(): Boolean {
    return packageManager.hasSystemFeature("android.hardware.type.television")
}

