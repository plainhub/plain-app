@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.ismartcoding.plain.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS keeps the Vue bundle in the app bundle, so it is copied into the caches
 * directory once per app version. Rust then serves it like any other file.
 */
internal actual fun extractWebAssets(): String {
    val version = getAppVersion().ifEmpty { "dev" }
    val caches = NSSearchPathForDirectoriesInDomains(
        NSCachesDirectory,
        NSUserDomainMask,
        true,
    ).firstOrNull() as? String ?: return ""
    val root = "$caches/web-assets/$version"
    val manager = NSFileManager.defaultManager
    if (manager.fileExistsAtPath("$root/.complete")) return root
    try {
        manager.removeItemAtPath(root, error = null)
        val source = NSBundle.mainBundle.resourcePath ?: return ""
        val destination = NSURL.fileURLWithPath(root)
        if (!manager.copyItemAtURL(NSURL.fileURLWithPath("$source/web"), destination, error = null)) return ""
        manager.createFileAtPath("$root/.complete", contents = null, attributes = null)
    } catch (_: Exception) {
        return ""
    }
    return root
}
