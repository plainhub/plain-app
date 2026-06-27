package com.ismartcoding.plain.platform

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ismartcoding.plain.appContextValue
import java.io.File

actual fun launchUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    appContextValue?.startActivity(intent)
}

actual fun openAppSettings() {
    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", appContextValue?.packageName ?: return, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    appContextValue?.startActivity(intent)
}

actual fun shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(intent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    appContextValue?.startActivity(chooser)
}

actual fun shareFile(path: String) {
    val ctx = appContextValue ?: return
    val file = File(path)
    val authority = "${ctx.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(ctx, authority, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = ctx.contentResolver.getType(uri) ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(chooser)
}