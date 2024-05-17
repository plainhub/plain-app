package com.ismartcoding.plain.ui.preview

import android.content.Context
import android.net.Uri
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.lib.extensions.getFileName
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.helpers.ValidateHelper
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.VideoHelper

data class PreviewItem(
    val id: String,
    val uri: Uri,
    var path: String = "",
    var size: Long = 0L,
    val mediaId: String = "",
    val data: IData? = null,
) {
    var intrinsicSize: IntSize = IntSize.Zero
    var rotation: Int = -1

    fun isWebUrl(): Boolean {
        return ValidateHelper.isUrl(path)
    }

    fun initAsync(context: Context, width: Int, height: Int) {
        if (path.isImageFast()) {
            rotation = ImageHelper.getRotation(path)
        }
        if (width > 0 && height > 0) {
            if (rotation == 90 || rotation == 270) {
                intrinsicSize = IntSize(height, width)
            } else {
                intrinsicSize = IntSize(width, height)
            }
        } else {
            intrinsicSize = if (path.isImageFast()) {
                ImageHelper.getIntrinsicSize(path, rotation)
            } else {
                VideoHelper.getIntrinsicSize(context, path)
            }
        }
    }

    fun initAsync(context: Context, item: DMessageFile) {
        if (path.isImageFast()) {
            rotation = ImageHelper.getRotation(path)
        }
        if (item.width > 0 && item.height > 0) {
            intrinsicSize = IntSize(item.width, item.height)
        } else {
            intrinsicSize = if (path.isImageFast()) {
                ImageHelper.getIntrinsicSize(path, rotation)
            } else {
                VideoHelper.getIntrinsicSize(context, path)
            }
        }
    }

    fun itemType(context: Context): Int {
        return when {
            uri.getFileName(context).isVideoFast() -> ItemType.VIDEO
            else -> ItemType.IMAGE
        }
    }
}

object ItemType {
    const val UNKNOWN = -1
    const val IMAGE = 2
    const val VIDEO = 3
}
