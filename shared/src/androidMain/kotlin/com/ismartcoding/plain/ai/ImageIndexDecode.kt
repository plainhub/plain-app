package com.ismartcoding.plain.ai

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import com.ismartcoding.plain.appContext
import java.io.File

internal object ImageIndexDecode {
    fun decode(path: String): String? {
        var staged: File? = null
        return try {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(File(path))) { decoder, info, _ ->
                require(info.size.width <= 32768 && info.size.height <= 32768)
                val pixels = info.size.width.toLong() * info.size.height
                if (pixels > 32L * 1024 * 1024) decoder.setTargetSampleSize(kotlin.math.ceil(kotlin.math.sqrt(pixels.toDouble() / (32L * 1024 * 1024))).toInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            try {
                val file = File.createTempFile("image-index-", ".png", appContext.cacheDir)
                staged = file
                file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                file.absolutePath
            } finally { bitmap.recycle() }
        } catch (_: Exception) { staged?.delete(); null }
    }
    fun release(path: String): Boolean {
        val file = File(path)
        require(file.parentFile?.canonicalFile == appContext.cacheDir.canonicalFile && file.name.startsWith("image-index-"))
        return !file.exists() || file.delete()
    }
}
