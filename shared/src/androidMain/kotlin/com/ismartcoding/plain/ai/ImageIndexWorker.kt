package com.ismartcoding.plain.ai

import android.graphics.Bitmap

interface ImageIndexWorker : AutoCloseable {
    fun embedBitmap(bitmap: Bitmap): FloatArray?
}
