package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DLinkPreview

/**
 * Fetch link previews for [urls] concurrently. Returns a list of [DLinkPreview]
 * (one per URL), with `hasError = true` for URLs that could not be fetched.
 *
 * On Android this downloads the HTML, parses Open Graph / title / favicon meta
 * tags and downloads a preview image into app storage. On iOS this returns an
 * empty list (no equivalent implementation yet).
 */
expect suspend fun fetchLinkPreviewsAsync(urls: List<String>): List<DLinkPreview>

/**
 * Delete the preview image cached at [imagePath] (an `app://` URI or filesystem
 * path). No-op on iOS.
 */
expect fun deletePreviewImage(imagePath: String)
