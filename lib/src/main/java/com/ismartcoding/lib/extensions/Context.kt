package com.ismartcoding.lib.extensions

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.MediaStore
import android.telecom.TelecomManager
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.lib.isTPlus
import java.io.File
import kotlin.math.roundToInt

fun Context.px(
    @DimenRes dimen: Int,
): Int = resources.getDimension(dimen).toInt()

fun Context.dp(
    @DimenRes dimen: Int,
): Float = resources.getDimensionPixelSize(dimen) / resources.displayMetrics.density

fun Context.getTextWidth(text: String): Float = TextView(this).paint.measureText(text)

fun Context.dp2px(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

fun Context.px2dp(px: Float): Int = (px / resources.displayMetrics.density).toInt()

fun dp2px(dp: Int): Int {
    val density: Float = Resources.getSystem().displayMetrics.density
    return (dp * density).roundToInt()
}

fun Context.getWindowHeight(): Int {
    if (isRPlus()) {
        return getSystemService(WindowManager::class.java).currentWindowMetrics.bounds.height()
    }

    val outMetrics = DisplayMetrics()
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    @Suppress("DEPRECATION")
    windowManager.defaultDisplay.getMetrics(outMetrics)
    return outMetrics.heightPixels
}

fun Context.getWindowWidth(): Int {
    if (isRPlus()) {
        return getSystemService(WindowManager::class.java).currentWindowMetrics.bounds.width()
    }

    val outMetrics = DisplayMetrics()
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    @Suppress("DEPRECATION")
    windowManager.defaultDisplay.getMetrics(outMetrics)
    return outMetrics.widthPixels
}

fun Context.getDrawableId(name: String): Int {
    return resources.getIdentifier(name, "drawable", packageName)
}

fun Context.hasPermission(vararg permission: String): Boolean {
    return permission.toSet().all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
}

fun Context.queryCursor(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    callback: (cursor: Cursor, indexCache: MutableMap<String, Int>) -> Unit,
) {
    contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val cache = mutableMapOf<String, Int>()
            do {
                callback(cursor, cache)
            } while (cursor.moveToNext())
        }
    }
}

fun Context.count(
    uri: Uri,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
): Int {
    var result = 0
    contentResolver.query(
        uri,
        arrayOf("count(*) AS count"),
        selection,
        selectionArgs,
        null,
    )?.run {
        moveToFirst()
        if (count > 0) {
            result = getInt(0)
        }
        close()
    }
    return result
}

fun Context.count2(
    uri: Uri,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
): Int {
    var result = 0
    contentResolver.query(
        uri,
        null,
        selection,
        selectionArgs,
        null,
    )?.run {
        moveToFirst()
        result = count
        close()
    }
    return result
}

fun Context.contentCount(
    uri: Uri,
    projection: Array<String> = arrayOf("_id"),
    selection: String? = null,
    selectionArgs: Array<String>? = null,
): Int {
    var result = 0
    contentResolver.query(
        uri,
        projection,
        selection,
        selectionArgs,
        null,
    )?.run {
        result = count
    }
    return result
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

fun Context.getUriMimeType(
    path: String,
    newUri: Uri,
): String {
    var mimeType = path.getMimeType()
    if (mimeType.isEmpty()) {
        mimeType = getMimeTypeFromUri(newUri)
    }
    return mimeType
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

fun Context.getMediaContentUri(path: String): Uri? {
    val uri =
        when {
            path.isImageFast() -> if (isQPlus()) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            path.isVideoFast() -> if (isQPlus()) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            path.isAudioFast() -> if (isQPlus()) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

    return getMediaContent(path, uri)
}

fun Context.getMediaContentUri(path: String, id: String): Uri? {
    val basUri =
        when {
            path.isImageFast() -> if (isQPlus()) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            path.isVideoFast() -> if (isQPlus()) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            path.isAudioFast() -> if (isQPlus()) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

    return Uri.withAppendedPath(basUri, id)
}

fun Context.getMediaContent(
    path: String,
    baseUri: Uri,
): Uri? {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = MediaStore.Images.Media.DATA + "= ?"
    val selectionArgs = arrayOf(path)
    try {
        val cursor = contentResolver.query(baseUri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val cache = mutableMapOf<String, Int>()
                val id = cursor.getStringValue(MediaStore.Images.Media._ID, cache)
                return Uri.withAppendedPath(baseUri, id)
            }
        }
    } catch (e: Exception) {
    }
    return null
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

fun Context.getNavigationBarHeight(): Int {
    return if (isGestureInteractionMode()) {
        16
    } else {
        48
    }
}

val Context.actionBarSize
    get() =
        theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
            .let { attrs -> attrs.getDimension(0, 0F).toInt().also { attrs.recycle() } }

fun Context.hasPermissions(vararg permissions: String): Boolean {
    return permissions
        .map { permission -> ActivityCompat.checkSelfPermission(this, permission) }
        .all { result -> result == PackageManager.PERMISSION_GRANTED }
}

fun Context.hasPermissionInManifest(vararg permissions: String): Boolean {
    val packageInfo =
        if (isTPlus()) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }
    return packageInfo.requestedPermissions?.any { permissions.contains(it) } ?: false
}

fun Context.isPortrait(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}

fun Context.isTV(): Boolean {
    return packageManager.hasSystemFeature("android.hardware.type.television")
}

