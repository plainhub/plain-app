package com.ismartcoding.plain.ui.shares.browser

import com.ismartcoding.plain.models.File
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import kotlinx.coroutines.CancellationException

internal object SharedThumbCache {
    fun isThumbable(entry: File): Boolean = !entry.isDir && (entry.name.isImageFast() || entry.name.isVideoFast())
    suspend fun thumbModel(entry: File): Any? {
        if (!isThumbable(entry)) return null
        return try { SharePreview.copy(entry, thumbnail = true) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
    }
}
