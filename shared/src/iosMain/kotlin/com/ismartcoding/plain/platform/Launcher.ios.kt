package com.ismartcoding.plain.platform

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
    // Requires UIViewController to present UIActivityViewController.
    // Phase 25 wires this via Compose LocalContext -> UIViewControllerRepresentable.
}

actual fun shareFile(path: String) {
    // Same as shareText: needs UIViewController present; implemented in Phase 25.
}

actual fun shareFiles(paths: List<String>) {
    // Needs UIViewController to present UIActivityViewController; implemented in Phase 25.
}

actual fun openFileExternal(path: String) {
    // Needs UIDocumentInteractionController; implemented in Phase 25.
}

actual fun relaunchApp() {
    // iOS apps cannot programmatically restart; no-op.
}