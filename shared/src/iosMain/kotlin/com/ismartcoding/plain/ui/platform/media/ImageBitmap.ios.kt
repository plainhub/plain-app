package com.ismartcoding.plain.ui.platform.media

import androidx.compose.ui.graphics.ImageBitmap
import coil3.toBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap

internal actual suspend fun decodeManagedImageBitmap(resource: com.ismartcoding.plain.models.ManagedResource): ImageBitmap? {
    val context = coil3.PlatformContext.INSTANCE
    val result = coil3.SingletonImageLoader.get(context).execute(coil3.request.ImageRequest.Builder(context).data(resource).build())
    return result.image?.toBitmap()?.asComposeImageBitmap()
}
