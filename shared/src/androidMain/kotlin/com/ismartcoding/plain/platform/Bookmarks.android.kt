package com.ismartcoding.plain.platform

import android.os.Environment
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.db.DBookmark
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import java.io.File
import java.net.URL

actual fun deleteBookmarkFavicons(bookmarks: List<DBookmark>) {
    bookmarks.forEach { b ->
        try {
            val path = b.faviconPath
            if (path.startsWith("app://")) {
                val rel = path.removePrefix("app://")
                val dir = File(appDir())
                val parent = dir.parentFile ?: return@forEach
                File(parent, rel).delete()
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}

actual suspend fun downloadBookmarkFavicon(faviconUrl: String, pageUrl: String): String? {
    return try {
        withIO {
            val client = KtorClientFactory.browserClient()
            val resp = client.get(faviconUrl)
            if (!resp.status.isSuccess()) { client.close(); return@withIO null }
            val bytes = resp.readRawBytes()
            client.close()
            if (bytes.isEmpty()) return@withIO null

            val ext = when {
                faviconUrl.endsWith(".png", ignoreCase = true) -> "png"
                faviconUrl.endsWith(".ico", ignoreCase = true) -> "ico"
                faviconUrl.endsWith(".svg", ignoreCase = true) -> "svg"
                else -> "ico"
            }
            val host = try { URL(pageUrl).host } catch (e: Exception) { "unknown" }
            val fileName = "bm_favicon_${host.replace(".", "_")}.${ext}"
            val dir = File(appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "bookmark_favicons")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            file.writeBytes(bytes)
            "app://${Environment.DIRECTORY_PICTURES}/bookmark_favicons/${fileName}"
        }
    } catch (e: Exception) {
        LogCat.e("downloadBookmarkFavicon: ${e.message}")
        null
    }
}
