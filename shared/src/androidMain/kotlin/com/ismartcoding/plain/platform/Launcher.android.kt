package com.ismartcoding.plain.platform

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ismartcoding.plain.appContextValue
import com.ismartcoding.plain.AppIntents
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
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

actual fun shareFile(path: String, email: String, subject: String) {
    val ctx = appContextValue ?: return
    ShareHelper.shareFile(ctx, File(path), email = email, subject = subject)
}

actual fun shareFileAs(path: String, displayName: String) {
    val ctx = appContextValue ?: return
    ShareHelper.shareFile(ctx, File(path), displayName = displayName)
}

actual fun shareFiles(paths: List<String>) {
    val ctx = appContextValue ?: return
    if (paths.isEmpty()) return
    ShareHelper.shareFiles(ctx, paths.map { File(it) })
}

actual fun openFileExternal(path: String) {
    val ctx = appContextValue ?: return
    val file = File(path)
    val uri = try {
        FileProvider.getUriForFile(ctx, AppIntents.AUTHORITY, file)
    } catch (e: IllegalArgumentException) {
        DialogHelper.showErrorMessage(LocaleHelper.getString(Res.string.cannot_share_file_not_accessible))
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = uri
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}

actual fun isFileShareable(path: String): Boolean {
    val file = File(path)
    if (!file.exists() || !file.canRead()) return false
    if (path.startsWith("/apex/")) return false
    if (path.startsWith("/system/") || path.startsWith("/vendor/") || path.startsWith("/product/")) {
        return try { file.inputStream().use { it.read() }; true } catch (e: Exception) { false }
    }
    return true
}

actual fun relaunchApp() {
    val ctx = appContextValue ?: return
    com.ismartcoding.plain.helpers.AppHelper.relaunch(ctx)
}