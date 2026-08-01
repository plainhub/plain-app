package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.lib.logcat.LogCat
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual fun launchUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}

actual fun openAppSettings() {
    UIApplication.sharedApplication.openURL(NSURL.URLWithString(UIApplicationOpenSettingsURLString)!!)
}

actual fun shareText(text: String) {
    val controller = IosPlatformRegistry.shareController()
    if (controller == null) {
        LogCat.w("Launcher.ios: no share controller registered, shareText ignored")
        return
    }
    controller.shareText(text)
}

actual fun shareFile(path: String, email: String, subject: String) {
    val controller = IosPlatformRegistry.shareController()
    if (controller == null) {
        LogCat.w("Launcher.ios: no share controller registered, shareFile ignored")
        return
    }
    controller.shareFile(path, path.getMimeType().ifEmpty { "application/octet-stream" })
}

actual fun shareFileAs(path: String, displayName: String) {
    val controller = IosPlatformRegistry.shareController()
    if (controller == null) {
        LogCat.w("Launcher.ios: no share controller registered, shareFileAs ignored")
        return
    }
    controller.shareFile(path, path.getMimeType().ifEmpty { "application/octet-stream" })
}

actual fun shareFiles(paths: List<String>) {
    val controller = IosPlatformRegistry.shareController()
    if (controller == null) {
        LogCat.w("Launcher.ios: no share controller registered, shareFiles ignored")
        return
    }
    controller.shareFiles(paths, paths.map { it.getMimeType().ifEmpty { "application/octet-stream" } })
}

actual fun openFileExternal(path: String) {
    val controller = IosPlatformRegistry.shareController()
    if (controller == null) {
        LogCat.w("Launcher.ios: no share controller registered, openFileExternal ignored")
        return
    }
    controller.openFileExternal(path)
}

actual fun isFileShareable(path: String): Boolean {
    if (path.startsWith("http://", true) || path.startsWith("https://", true)) return true
    if (path.startsWith("file://", true)) return true
    if (path.startsWith("/")) return true
    return false
}

actual fun relaunchApp() {
    // iOS apps cannot programmatically restart; no-op by design.
}
