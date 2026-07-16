package com.ismartcoding.plain.platform

import android.graphics.Bitmap
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.BitmapHelper
import com.ismartcoding.plain.lib.logcat.LogCat

actual suspend fun combineBitmapGrid(paths: List<String>, size: Int): Any? {
    if (paths.isEmpty()) return null
    val bitmaps = mutableListOf<Bitmap>()
    paths.forEach { path ->
        try {
            val bm = BitmapHelper.decodeBitmapFromFileAsync(appContext, path, size, size)
            if (bm != null) {
                bitmaps.add(bm)
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }
    }
    return try {
        val softwareBitmaps = bitmaps.map { it.copy(Bitmap.Config.ARGB_8888, true) }
        CombineBitmapTools.combineBitmap(size, size, softwareBitmaps)
    } catch (ex: Exception) {
        LogCat.e(ex.toString())
        null
    }
}
