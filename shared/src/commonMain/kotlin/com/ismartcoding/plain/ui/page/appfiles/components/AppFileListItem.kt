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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.getExtensionFromMimeType
import com.ismartcoding.plain.platform.getFileIconPath
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.platform.TransformImageView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.models.VAppFile
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun AppFileListItem(
    file: VAppFile,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val fileName = file.fileName
    val extension = fileName.getFilenameExtension().ifEmpty {
        getExtensionFromMimeType(file.appFile.mimeType)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.cardBackgroundNormal,
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
                val widthPx = with(density) { 40.dp.toPx() }.toInt()
                TransformImageView(
                    modifier = Modifier
                        .size(40.dp),
                    path = file.appFile.realPath,
                    fileName = fileName,
                    key = file.appFile.id,
                    itemState = itemState,
                    previewerState = previewerState,
                    widthPx = widthPx,
                    forceVideoDecoder = isVideo,
                )
            } else {
                AsyncImage(
                    model = getFileIconPath(extension),
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
