package com.ismartcoding.plain.thumbnail

import android.content.Context
import java.io.File

interface ThumbnailProvider {
    suspend fun toThumbBytesAsync(
        context: Context,
        file: File,
        width: Int,
        height: Int,
        centerCrop: Boolean,
        mediaId: String,
        fileName: String = "",
    ): ByteArray?

    companion object {
        var instance: ThumbnailProvider? = null
    }
}
