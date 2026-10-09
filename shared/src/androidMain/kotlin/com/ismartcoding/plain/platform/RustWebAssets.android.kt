package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import java.io.File

/**
 * The bundle ships as APK assets, which are not a filesystem path, so the web
 * UI is unpacked once per app version. 11 MB / 1000+ files — the copy is
 * skipped entirely when the stamped directory is already complete.
 */
internal actual fun extractWebAssets(): String {
    val context = appContext
    val version = getAppVersion().ifEmpty { "dev" }
    val root = File(context.filesDir, "web-assets/$version")
    if (File(root, ".complete").isFile) return root.absolutePath
    root.deleteRecursively()
    copyAssetDir(context, "web", root)
    if (!File(root, "index.html").isFile) return ""
    File(root, ".complete").writeText("")
    return root.absolutePath
}

private fun copyAssetDir(context: android.content.Context, assetPath: String, target: File) {
    val children = context.assets.list(assetPath) ?: return
    if (children.isEmpty()) {
        context.assets.open("$assetPath/").use { it.copyTo(target.outputStream()) }
        return
    }
    target.mkdirs()
    children.forEach { child ->
        if (child.contains('.')) {
            context.assets.open("$assetPath/$child").use { input ->
                File(target, child).outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            copyAssetDir(context, "$assetPath/$child", File(target, child))
        }
    }
}
