package com.ismartcoding.plain.features

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.KtorClientFactory
import com.ismartcoding.plain.platform.deleteBookmarkFavicons
import com.ismartcoding.plain.platform.downloadBookmarkFavicon
import com.ismartcoding.plain.db.DBookmark
import com.ismartcoding.plain.db.DBookmarkGroup
import com.ismartcoding.plain.helpers.TimeHelper
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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

    suspend fun deleteBookmarks(ids: Set<String>) = withIO {
        val bookmarks = ids.mapNotNull { AppDatabase.instance.bookmarkDao().getById(it) }
            .filter { it.faviconPath.isNotEmpty() }
        deleteBookmarkFavicons(bookmarks)
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
    suspend fun fetchAndUpdateSingle(bookmarkId: String): DBookmark? = withIO {
        val b = AppDatabase.instance.bookmarkDao().getById(bookmarkId) ?: return@withIO null
        return@withIO try {
            val result = fetchPageMeta(b.url)
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
    suspend fun fetchMetadataAsync(bookmarkIds: List<String>) = withIO {
        if (bookmarkIds.isEmpty()) return@withIO
        try {
            coroutineScope {
                bookmarkIds.map { id ->
                    async {
                        val b = AppDatabase.instance.bookmarkDao().getById(id) ?: return@async
                        val result = fetchPageMeta(b.url)
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
    private suspend fun fetchPageMeta(url: String): Pair<String?, String?> {
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
                val localPath = if (faviconUrl != null) downloadBookmarkFavicon(faviconUrl, url) else null
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
            val base = Url(pageUrl)
            "${base.protocol.name}://${base.host}/favicon.ico"
        } catch (e: Exception) { null }
    }

    private fun resolveUrl(base: String, url: String): String {
        return try {
            when {
                url.startsWith("http://") || url.startsWith("https://") -> url
                url.startsWith("//") -> "${Url(base).protocol.name}:$url"
                url.startsWith("/") -> {
                    val b = Url(base)
                    val portStr = if (b.specifiedPort != DEFAULT_PORT) ":${b.specifiedPort}" else ""
                    "${b.protocol.name}://${b.host}${portStr}$url"
                }
                else -> url
            }
        } catch (e: Exception) { url }
    }

    private const val DEFAULT_PORT = -1
}
