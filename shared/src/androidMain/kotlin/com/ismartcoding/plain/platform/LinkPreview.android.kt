package com.ismartcoding.plain.platform

import android.graphics.BitmapFactory
import com.ismartcoding.plain.helpers.AppFileStore

actual suspend fun getImageDimensions(data: ByteArray): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, options)
    return options.outWidth to options.outHeight
}

actual suspend fun importImageBytesToFid(data: ByteArray, mimeType: String): String? {
    val dFile = AppFileStore.importBytes(data, mimeType)
    return AppFileStore.toFidUri(dFile.id, AppFileStore.extFromMime(dFile.mimeType))
}