package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DBookmark
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.lib.logcat.LogCat
import io.ktor.http.Url

/**
 * Absolute directory in which bookmark favicons are stored. Platform-specific
 * because Android places it under the external pictures directory while iOS
 * uses the app sandbox. The directory may not yet exist; callers create it via
 * [ensureParentDir] before writing.
 */
expect fun bookmarkFaviconDirPath(): String

/**
 * Delete the favicon files associated with the given [bookmarks]. Files are
 * resolved through the canonical `app://` path mapping ([getFinalPath]).
 */
fun deleteBookmarkFavicons(bookmarks: List<DBookmark>) {
    bookmarks.forEach { b ->
        try {
            val path = b.faviconPath
            if (path.startsWith("app://")) {
                deleteFileAt(path.getFinalPath())
            }
        } catch (_: Exception) {
            // ignore individual failures so one bad entry doesn't abort the rest
        }
    }
}

/**
 * Download the favicon at [faviconUrl] and store it under the bookmark favicon
 * directory. Returns the canonical `app://` URI used to reference the file
 * later, or null on failure.
 */
suspend fun downloadBookmarkFavicon(faviconUrl: String, pageUrl: String): String? {
    return try {
        withIO {
            val client = createBrowserHttpClient()
            val resp = client.get(faviconUrl)
            resp.use {
                if (!it.isSuccess()) return@withIO null
                val bytes = it.bodyAsBytes()
                if (bytes.isEmpty()) return@withIO null

                val ext = when {
                    faviconUrl.endsWith(".png", ignoreCase = true) -> "png"
                    faviconUrl.endsWith(".ico", ignoreCase = true) -> "ico"
                    faviconUrl.endsWith(".svg", ignoreCase = true) -> "svg"
                    else -> "ico"
                }
                val host = try { Url(pageUrl).host } catch (_: Exception) { "unknown" }
                val fileName = "bm_favicon_${host.replace(".", "_")}.$ext"
                val dir = bookmarkFaviconDirPath()
                val absPath = "$dir/$fileName"
                ensureParentDir(absPath)
                if (!writeBytesToPath(absPath, bytes)) return@withIO null
                val rel = dir.removePrefix(appDir()).removePrefix("/")
                "app://$rel/$fileName"
            }
        }
    } catch (e: Exception) {
        LogCat.e("downloadBookmarkFavicon: ${e.message}")
        null
    }
}
