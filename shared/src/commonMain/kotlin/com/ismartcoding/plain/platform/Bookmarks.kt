package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DBookmark

/**
 * Delete the favicon files associated with the given [bookmarks].
 * No-op on platforms without a filesystem-backed favicon store.
 */
expect fun deleteBookmarkFavicons(bookmarks: List<DBookmark>)

/**
 * Download the favicon at [faviconUrl] and store it under the bookmark favicon directory.
 * Returns the canonical app:// URI string used to reference the file later, or null on failure.
 *
 * The implementation is platform-specific because the destination directory is derived from
 * the platform's external pictures directory (Android) or app sandbox (iOS).
 */
expect suspend fun downloadBookmarkFavicon(faviconUrl: String, pageUrl: String): String?
