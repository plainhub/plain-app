package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DLinkPreview

actual suspend fun fetchLinkPreviewsAsync(urls: List<String>): List<DLinkPreview> {
    // iOS stub: no link preview fetching yet
    return emptyList()
}

actual fun deletePreviewImage(imagePath: String) {
    // iOS stub: no-op
}
