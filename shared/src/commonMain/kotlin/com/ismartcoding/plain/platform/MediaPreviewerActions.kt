package com.ismartcoding.plain.platform

import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem

expect val canSavePreviewMedia: Boolean

expect suspend fun sharePreviewMedia(m: PreviewItem)

/**
 * Saves the previewed media to the device.
 * [dir] == null saves to the system Downloads directory; otherwise [dir] is
 * the destination folder chosen by the user (SaveToSheet).
 */
expect suspend fun savePreviewMedia(m: PreviewItem, dir: String?)
