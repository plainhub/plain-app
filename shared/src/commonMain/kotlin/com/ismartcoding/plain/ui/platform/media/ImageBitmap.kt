package com.ismartcoding.plain.ui.platform.media

import androidx.compose.ui.graphics.ImageBitmap

internal expect fun decodeImageFileBitmap(path: String): ImageBitmap?

internal expect suspend fun decodeManagedImageBitmap(resource: com.ismartcoding.plain.models.ManagedResource): ImageBitmap?
