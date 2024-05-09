package com.ismartcoding.plain.ui.preview

import android.content.Context
import android.net.Uri
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.toSize
import com.ismartcoding.lib.extensions.getFileName
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.helpers.CoroutinesHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.MediaHelper
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

data class PreviewItem(
    val id: String,
    val uri: Uri,
    val path: String = "",
    val size: Long = 0L,
) {
    var intrinsicSize: Size = Size.Zero
    var rotation: Int = -1

    fun initAsync(context: Context, width: Int, height: Int) {
        rotation = ImageHelper.getImageRotation(path)
        if (width > 0 && height > 0) {
            intrinsicSize = Size(width.toFloat(), height.toFloat())
        } else {
            intrinsicSize = if (path.isImageFast()) MediaHelper.getImageIntrinsicSize(path).toSize() else MediaHelper.getVideoIntrinsicSize(context, path).toSize()
        }
        LogCat.d("intrinsicSize: $intrinsicSize")
        if (rotation == 90 || rotation == 270) {
            LogCat.d("intrinsicSize: 2")
            intrinsicSize = Size(intrinsicSize.height, intrinsicSize.width)
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
