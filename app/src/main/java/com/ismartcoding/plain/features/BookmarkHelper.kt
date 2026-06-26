package com.ismartcoding.plain.features

import android.content.Context
import android.os.Environment
import com.ismartcoding.plain.lib.extensions.appDir
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.api.KtorClientFactory
import com.ismartcoding.plain.api.OkHttpClientFactory
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DBookmark
import com.ismartcoding.plain.db.DBookmarkGroup
import com.ismartcoding.plain.helpers.TimeHelper
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.net.URL

object BookmarkHelper {

    // ─── Bookmark Group CRUD ──────────────────────────────────────────────────

    suspend fun getAllGroups(): List<DBookmarkGroup> = withIO {
        AppDatabase.instance.bookmarkGroupDao().getAll()
    }

    suspend fun getGroupById(id: String): DBookmarkGroup? = withIO {
        AppDatabase.instance.bookmarkGroupDao().getById(id)
    }

    suspend fun createGroup(name: String): DBookmarkGroup = withIO {
        val group = DBookmarkGroup().apply { this.name = name }
        AppDatabase.instance.bookmarkGroupDao().insert(group)
        group
    }

    suspend fun updateGroup(id: String, block: DBookmarkGroup.() -> Unit): DBookmarkGroup? = withIO {
        val group = AppDatabase.instance.bookmarkGroupDao().getById(id) ?: return@withIO null
        group.apply(block)
        group.updatedAt = TimeHelper.now()
        AppDatabase.instance.bookmarkGroupDao().update(group)
        group
    }

    suspend fun deleteGroup(id: String) = withIO {
        AppDatabase.instance.bookmarkGroupDao().delete(setOf(id))
        val bookmarks = AppDatabase.instance.bookmarkDao().getByGroupId(id)
        bookmarks.forEach { b ->
            b.groupId = ""
            b.updatedAt = TimeHelper.now()
            AppDatabase.instance.bookmarkDao().update(b)
        }
    }

    // ─── Bookmark CRUD ────────────────────────────────────────────────────────

    suspend fun getAll(): List<DBookmark> = withIO {
        AppDatabase.instance.bookmarkDao().getAll()
    }

    suspend fun getById(id: String): DBookmark? = withIO {
        AppDatabase.instance.bookmarkDao().getById(id)
    }

    /**
     * Batch-add bookmarks from a list of URLs.
     * Returns the newly created bookmarks so callers can trigger metadata fetch.
     */
    suspend fun addBookmarks(urls: List<String>, groupId: String = ""): List<DBookmark> = withIO {
        val created = mutableListOf<DBookmark>()
        urls.forEach { url ->
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return@forEach
            val bookmark = DBookmark().apply {
                this.url = trimmed
                this.title = trimmed          // placeholder until metadata arrives
                this.groupId = groupId
            }
            AppDatabase.instance.bookmarkDao().insert(bookmark)
            created.add(bookmark)
        }
        created
    }

    suspend fun updateBookmark(id: String, block: DBookmark.() -> Unit): DBookmark? = withIO {
        val bookmark = AppDatabase.instance.bookmarkDao().getById(id) ?: return@withIO null
        bookmark.apply(block)
        bookmark.updatedAt = TimeHelper.now()
        AppDatabase.instance.bookmarkDao().update(bookmark)
        bookmark
    }

    suspend fun deleteBookmarks(ids: Set<String>, context: Context) = withIO {
        ids.forEach { id ->
            val b = AppDatabase.instance.bookmarkDao().getById(id)
            if (b != null && b.faviconPath.isNotEmpty()) {
                deleteFaviconFile(context, b.faviconPath)
            }
        }
        AppDatabase.instance.bookmarkDao().delete(ids)
    }

    suspend fun recordClick(id: String) = withIO {
        val bookmark = AppDatabase.instance.bookmarkDao().getById(id) ?: return@withIO
        bookmark.clickCount++
        bookmark.lastClickedAt = TimeHelper.now()
        bookmark.updatedAt = TimeHelper.now()
        AppDatabase.instance.bookmarkDao().update(bookmark)
    }

    // ─── Metadata fetching ────────────────────────────────────────────────────

    /**
     * Fetch title and favicon for a single bookmark (called via FetchBookmarkMetadataEvent).
     * Returns the updated DBookmark if any field changed so the caller can push a WebSocket event,
     * or null if nothing changed.
     */
    suspend fun fetchAndUpdateSingle(context: Context, bookmarkId: String): DBookmark? = withIO {
        val b = AppDatabase.instance.bookmarkDao().getById(bookmarkId) ?: return@withIO null
        return@withIO try {
            val result = fetchPageMeta(context, b.url)
            var changed = false
            result.first?.takeIf { it.isNotEmpty() }?.let {
                if (b.title != it) { b.title = it; changed = true }
            }
            result.second?.let {
                if (b.faviconPath != it) { b.faviconPath = it; changed = true }
            }
            if (!changed) return@withIO null
            b.updatedAt = TimeHelper.now()
            AppDatabase.instance.bookmarkDao().update(b)
            b
        } catch (e: Exception) {
            LogCat.e("BookmarkHelper.fetchAndUpdateSingle($bookmarkId): ${e.message}")
            null
        }
    }

    /**
     * Fetch title and favicon for each newly created bookmark.
     * Mirrors the pattern used in FetchLinkPreviewsEvent / ChatHelper.
     */
    suspend fun fetchMetadataAsync(context: Context, bookmarkIds: List<String>) = withIO {
        if (bookmarkIds.isEmpty()) return@withIO
        try {
            coroutineScope {
                bookmarkIds.map { id ->
                    async {
                        val b = AppDatabase.instance.bookmarkDao().getById(id) ?: return@async
                        val result = fetchPageMeta(context, b.url)
                        result.first?.takeIf { it.isNotEmpty() }?.let { b.title = it }
                        result.second?.let { b.faviconPath = it }
                        b.updatedAt = TimeHelper.now()
                        AppDatabase.instance.bookmarkDao().update(b)
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            LogCat.e("BookmarkHelper.fetchMetadataAsync: ${e.message}")
        }
    }

    /** Returns Pair(title, localFaviconPath) */
    private suspend fun fetchPageMeta(context: Context, url: String): Pair<String?, String?> {
        return withIO {
            try {
                val client = KtorClientFactory.browserClient()
                val response = client.get(url)
                if (!response.status.isSuccess()) {
                    client.close()
                    return@withIO Pair(null, null)
                }
                val contentType = response.headers["Content-Type"]?.lowercase() ?: ""
                if (!contentType.contains("text/html")) {
                    client.close()
                    return@withIO Pair(null, null)
                }
                val html = response.bodyAsText()
                client.close()

                val title = extractTitle(html)
                val faviconUrl = extractFaviconUrl(url, html)
                val localPath = if (faviconUrl != null) downloadFavicon(context, faviconUrl, url) else null
                Pair(title, localPath)
            } catch (e: Exception) {
                LogCat.e("BookmarkHelper.fetchPageMeta($url): ${e.message}")
                Pair(null, null)
            }
        }
    }

    private fun extractTitle(html: String): String? {
        // og:title takes priority
        val ogTitle = Regex("<meta[^>]+property=[\"']og:title[\"'][^>]+content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(html)
        if (ogTitle != null) return ogTitle.groupValues[1].trim().take(200)
        val tag = Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE).find(html)
        return tag?.groupValues?.get(1)?.trim()?.take(200)
    }

    private fun extractFaviconUrl(pageUrl: String, html: String): String? {
        val patterns = listOf(
            "<link[^>]+rel=[\"'][^\"']*icon[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']",
            "<link[^>]+href=[\"']([^\"']+)[\"'][^>]+rel=[\"'][^\"']*icon[^\"']*[\"']",
            "<link[^>]+rel=[\"']shortcut icon[\"'][^>]+href=[\"']([^\"']+)[\"']",
            "<link[^>]+rel=[\"']apple-touch-icon[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']",
        )
        for (p in patterns) {
            val m = Regex(p, RegexOption.IGNORE_CASE).find(html) ?: continue
            return resolveUrl(pageUrl, m.groupValues[1].trim())
        }
        return try {
            val base = URL(pageUrl)
            "${base.protocol}://${base.host}/favicon.ico"
        } catch (e: Exception) { null }
    }

    private fun resolveUrl(base: String, url: String): String {
        return try {
            when {
                url.startsWith("http://") || url.startsWith("https://") -> url
                url.startsWith("//") -> "${URL(base).protocol}:$url"
                url.startsWith("/") -> {
                    val b = URL(base)
                    "${b.protocol}://${b.host}${if (b.port != -1) ":${b.port}" else ""}$url"
                }
                else -> url
            }
        } catch (e: Exception) { url }
    }

    private suspend fun downloadFavicon(context: Context, faviconUrl: String, pageUrl: String): String? {
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
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "bookmark_favicons")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                file.writeBytes(bytes)
                "app://${Environment.DIRECTORY_PICTURES}/bookmark_favicons/${fileName}"
            }
        } catch (e: Exception) {
            LogCat.e("BookmarkHelper.downloadFavicon: ${e.message}")
            null
        }
    }

    private fun deleteFaviconFile(context: Context, path: String) {
        try {
            if (path.startsWith("app://")) {
                val rel = path.removePrefix("app://")
                val dir = File(context.appDir())
                File(dir.parent ?: return, rel).delete()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

}
