package com.ismartcoding.lib.helpers

import android.content.Context

object AssetsHelper {
    fun read(
        context: Context,
        fileName: String,
    ): String {
        val inputStream = context.assets.open(fileName)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        return buffer.decodeToString()
    }
}
