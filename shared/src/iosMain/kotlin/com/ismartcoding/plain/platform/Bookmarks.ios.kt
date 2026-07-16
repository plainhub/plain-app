package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DBookmark

actual fun deleteBookmarkFavicons(bookmarks: List<DBookmark>) {}

actual suspend fun downloadBookmarkFavicon(faviconUrl: String, pageUrl: String): String? = null
