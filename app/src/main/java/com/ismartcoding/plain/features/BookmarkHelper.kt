package com.ismartcoding.plain.features

import android.content.Context
import android.os.Environment
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.api.HttpClientManager
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

    fun getAllGroups(): List<DBookmarkGroup> {
        return AppDatabase.instance.bookmarkGroupDao().getAll()
    }

    fun getGroupById(id: String): DBookmarkGroup? {
        return AppDatabase.instance.bookmarkGroupDao().getById(id)
    }

    fun createGroup(name: String): DBookmarkGroup {
        val group = DBookmarkGroup().apply { this.name = name }
        AppDatabase.instance.bookmarkGroupDao().insert(group)
        return group
    }

    fun updateGroup(id: String, block: DBookmarkGroup.() -> Unit): DBookmarkGroup? {
        val group = AppDatabase.instance.bookmarkGroupDao().getById(id) ?: return null
        group.apply(block)
        group.updatedAt = TimeHelper.now()
        AppDatabase.instance.bookmarkGroupDao().update(group)
        return group
    }

    fun deleteGroup(id: String) {
        AppDatabase.instance.bookmarkGroupDao().delete(setOf(id))
        // Move all bookmarks in this group to ungrouped
        val bookmarks = AppDatabase.instance.bookmarkDao().getByGroupId(id)
        bookmarks.forEach { b ->
            b.groupId = ""
            b.updatedAt = TimeHelper.now()
            AppDatabase.instance.bookmarkDao().update(b)
        }
    }

    // ─── Bookmark CRUD ────────────────────────────────────────────────────────

    fun getAll(): List<DBookmark> {
        return AppDatabase.instance.bookmarkDao().getAll()
    }

    fun getById(id: String): DBookmark? {
        return AppDatabase.instance.bookmarkDao().getById(id)
    }

    /**
     * Batch-add bookmarks from a list of URLs.
     * Returns the newly created bookmarks so callers can trigger metadata fetch.
     */
    fun addBookmarks(urls: List<String>, groupId: String = ""): List<DBookmark> {
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
        return created
    }

    fun updateBookmark(id: String, block: DBookmark.() -> Unit): DBookmark? {
        val bookmark = AppDatabase.instance.bookmarkDao().getById(id) ?: return null
        bookmark.apply(block)
        bookmark.updatedAt = TimeHelper.now()
        AppDatabase.instance.bookmarkDao().update(bookmark)
        return bookmark
    }

    fun deleteBookmarks(ids: Set<String>, context: Context) {
        ids.forEach { id ->
            val b = AppDatabase.instance.bookmarkDao().getById(id)
            if (b != null && b.faviconPath.isNotEmpty()) {
                deleteFaviconFile(context, b.faviconPath)
            }
        }
        AppDatabase.instance.bookmarkDao().delete(ids)
    }

    fun recordClick(id: String) {
        val bookmark = AppDatabase.instance.bookmarkDao().getById(id) ?: return
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
    suspend fun fetchAndUpdateSingle(context: Context, bookmarkId: String): DBookmark? {
        val b = AppDatabase.instance.bookmarkDao().getById(bookmarkId) ?: return null
        return try {
            val result = fetchPageMeta(context, b.url)
            var changed = false
            result.first?.takeIf { it.isNotEmpty() }?.let {
                if (b.title != it) { b.title = it; changed = true }
            }
            result.second?.let {
                if (b.faviconPath != it) { b.faviconPath = it; changed = true }
            }
            if (!changed) return null
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
    suspend fun fetchMetadataAsync(context: Context, bookmarkIds: List<String>) {
        if (bookmarkIds.isEmpty()) return
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
                val client = HttpClientManager.browserClient()
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
                val client = HttpClientManager.browserClient()
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
                val dir = context.getExternalFilesDir(null) ?: return
                File(dir.parent ?: return, rel).delete()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

}
