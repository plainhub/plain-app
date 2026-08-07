package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.platform.getImageIntrinsicSize
import com.ismartcoding.plain.platform.getImageRotation
import com.ismartcoding.plain.platform.getVideoMeta
import kotlin.text.ifEmpty

data class PreviewItem(
    val id: String,
    var path: String = "",
    var size: Long = 0L,
    val mediaId: String = "",
    val data: IData? = null, // DMessageFile, DVideo, DImage
) {
    var intrinsicSize: IntSize = IntSize.Zero
    var rotation: Int = -1

    fun isVideo(): Boolean {
        if (path.isVideoFast()) return true
        return when (val d = data) {
            is DMessageFile -> d.fileName.isVideoFast()
            is DVideo -> true
            else -> false
        }
    }

    fun getMimeType(): String {
       return path.getMimeType().ifEmpty {
            if (data is DMessageFile) data.fileName.getMimeType() else ""
        }
    }

    fun isImage(): Boolean {
        if (path.isImageFast()) return true
        return when (val d = data) {
            is DMessageFile -> d.fileName.isImageFast()
            is DImage -> true
            else -> false
        }
    }

    fun initAsync(m: DImage) {
        rotation = m.rotation
        intrinsicSize = m.getRotatedSize()
        if (intrinsicSize == IntSize.Zero) {
            initImageAsync()
        }
    }

    fun initImageAsync() {
        rotation = getImageRotation(path)
        intrinsicSize = getImageIntrinsicSize(path, rotation)
    }

    fun initVideoAsync() {
        resolveVideoIntrinsicSize()
    }

    private fun resolveVideoIntrinsicSize() {
        val meta = getVideoMeta(path) ?: return
        rotation = meta.rotation
        intrinsicSize = if (rotation == 90 || rotation == 270) {
            IntSize(meta.height, meta.width)
        } else {
            IntSize(meta.width, meta.height)
        }
    }

    fun initAsync(m: DVideo) {
        rotation = m.rotation
        intrinsicSize = m.getRotatedSize()
        if (intrinsicSize == IntSize.Zero) {
            resolveVideoIntrinsicSize()
        }
    }

    fun initAsync(item: DMessageFile) {
        if (item.fileName.isImageFast()) {
            rotation = getImageRotation(path)
            intrinsicSize = if (item.width > 0 && item.height > 0) {
                IntSize(item.width, item.height)
            } else {
                getImageIntrinsicSize(path, rotation)
            }
        } else {
            if (item.width > 0 && item.height > 0) {
                val meta = getVideoMeta(path) ?: return
                rotation = meta.rotation
                intrinsicSize = IntSize(item.width, item.height)
            } else {
                resolveVideoIntrinsicSize()
            }
        }
    }
}

object ItemType {
    const val UNKNOWN = -1
    const val IMAGE = 2
    const val VIDEO = 3
}
