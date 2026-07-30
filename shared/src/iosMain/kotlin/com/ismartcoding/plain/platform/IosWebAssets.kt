@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager

/**
 * Resolves static web assets (the Vue SPA built into `app/src/main/resources/web/`)
 * from the iOS app bundle.
 *
 * During the Xcode build a "Copy Files" build phase copies the `web/` folder
 * into the app bundle as a folder reference (blue folder in Xcode), so every
 * asset is reachable at `Bundle.main.bundlePath/web/<relative>`.
 *
 * This helper mirrors `staticResources("/", "web", ...)` from the Android
 * Ktor module — it maps a request path like `assets/app.js` to the bundle file
 * `web/assets/app.js`.
 */
object IosWebAssets {

    private val webRoot: String? by lazy {
        // The web folder is copied into the main bundle during the build phase.
        val candidate = NSBundle.mainBundle.resourcePath + "/web"
        if (NSFileManager.defaultManager.fileExistsAtPath(candidate)) {
            candidate
        } else {
            // Fall back to bundlePath (some Xcode configurations put resources there)
            val alt = NSBundle.mainBundle.bundlePath + "/web"
            if (NSFileManager.defaultManager.fileExistsAtPath(alt)) alt else null
        }
    }

    /**
     * Resolve [relativePath] (e.g. `assets/app.js`, `favicon.ico`, `index.html`)
     * to an absolute file path inside the app bundle. Returns `null` when the
     * file does not exist.
     */
    fun resolve(relativePath: String): String? {
        val root = webRoot ?: return null
        val fullPath = "$root/${relativePath.trimStart('/')}"
        if (!NSFileManager.defaultManager.fileExistsAtPath(fullPath)) return null
        return fullPath
    }

    /**
     * Map a file extension to a MIME content type. Covers the types used by
     * the web SPA (HTML, JS, CSS, fonts, images). Falls back to
     * `application/octet-stream`.
     */
    fun contentTypeFor(relativePath: String): String {
        val ext = relativePath.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "html", "htm" -> "text/html; charset=utf-8"
            "js", "mjs" -> "application/javascript; charset=utf-8"
            "css" -> "text/css; charset=utf-8"
            "json" -> "application/json; charset=utf-8"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "ico" -> "image/x-icon"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "wasm" -> "application/wasm"
            "txt" -> "text/plain; charset=utf-8"
            "map" -> "application/json; charset=utf-8"
            else -> "application/octet-stream"
        }
    }
}
