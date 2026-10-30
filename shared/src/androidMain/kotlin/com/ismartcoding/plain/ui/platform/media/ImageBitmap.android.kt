package com.ismartcoding.plain.ui.platform.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import coil3.toBitmap
import coil3.request.allowHardware

internal actual fun decodeImageFileBitmap(path: String): ImageBitmap? =
    BitmapFactory.decodeFile(path)?.asImageBitmap()

internal actual suspend fun decodeManagedImageBitmap(resource: com.ismartcoding.plain.models.ManagedResource): ImageBitmap? {
    val context = com.ismartcoding.plain.appContext
    val result = coil3.SingletonImageLoader.get(context).execute(coil3.request.ImageRequest.Builder(context).data(resource).allowHardware(false).build())
    return result.image?.toBitmap()?.asImageBitmap()
}
