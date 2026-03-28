package com.ismartcoding.plain.ui.page.appfiles.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.webkit.MimeTypeMap
import coil3.compose.AsyncImage
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformImageView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.plain.ui.models.VAppFile

@Composable
fun AppFileListItem(
    file: VAppFile,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val fileName = file.fileName
    val extension = fileName.getFilenameExtension().ifEmpty {
        MimeTypeMap.getSingleton().getExtensionFromMimeType(file.appFile.mimeType).orEmpty()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isImage = fileName.isImageFast() || file.appFile.mimeType.startsWith("image/")
            val isVideo = fileName.isVideoFast() || file.appFile.mimeType.startsWith("video/")

            if (isImage || isVideo) {
                TransformImageView(
                    modifier = Modifier
                        .size(40.dp),
                    path = file.appFile.realPath,
                    fileName = fileName,
                    key = file.appFile.id,
                    itemState = itemState,
                    previewerState = previewerState,
                    widthPx = context.dp2px(40),
                    forceVideoDecoder = isVideo,
                )
            } else {
                AsyncImage(
                    model = AppHelper.getFileIconPath(extension),
                    modifier = Modifier.size(40.dp),
                    contentDescription = fileName,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpace(2.dp)
                Text(
                    text = file.appFile.size.formatBytes() + "  ·  " + file.appFile.createdAt.formatDateTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
