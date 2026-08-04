package com.ismartcoding.plain.platform

import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem

expect val canSavePreviewMedia: Boolean

expect suspend fun sharePreviewMedia(m: PreviewItem)

expect suspend fun savePreviewMedia(m: PreviewItem)
