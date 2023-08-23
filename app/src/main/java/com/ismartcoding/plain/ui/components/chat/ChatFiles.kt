package com.ismartcoding.plain.ui.components.chat

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.size.Size
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isPdfFile
import com.ismartcoding.lib.extensions.isTextFile
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.audio.DPlaylistAudio
import com.ismartcoding.plain.services.AudioPlayerService
import com.ismartcoding.plain.ui.PdfViewerDialog
import com.ismartcoding.plain.ui.TextEditorDialog
import com.ismartcoding.plain.ui.audio.AudioPlayerDialog
import com.ismartcoding.plain.ui.base.PAsyncImage
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem

@Composable
fun ChatFiles(context: Context, m: VChat) {
    val fileItems = (m.value as DMessageFiles).items
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp)
            )
    ) {
        fileItems.forEachIndexed { index, item ->
            val path = item.uri.getFinalPath(context)
            Box(modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (path.isImageFast() || path.isVideoFast()) {
                        val items = fileItems
                            .filter { it.uri.isVideoFast() || it.uri.isImageFast() }
                        PreviewDialog().show(
                            items = items.mapIndexed { i, s -> PreviewItem(m.id + "|" + i, s.uri.getFinalPath(context)) },
                            initKey = m.id + "|" + items.indexOf(item),
                        )
                    } else if (path.isAudioFast()) {
                        AudioPlayerDialog().show()
                        Permissions.checkNotification(context, R.string.audio_notification_prompt) {
                            AudioPlayerService.play(context, DPlaylistAudio.fromPath(context, path))
                        }
                    } else if (path.isTextFile()) {
                        if (item.size <= Constants.MAX_READABLE_TEXT_FILE_SIZE) {
                            TextEditorDialog(path).show()
                        } else {
                            DialogHelper.showMessage(R.string.text_file_size_limit)
                        }
                    } else if (path.isPdfFile()) {
                        PdfViewerDialog(path).show()
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (index == 0) 12.dp else 6.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, end = 8.dp),
                            text = path.getFilenameFromPath(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            text = FormatHelper.formatBytes(item.size) + if (item.duration > 0) " / ${FormatHelper.formatDuration(item.duration)}" else "",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                        )
                    }
                    if (path.isImageFast() || path.isVideoFast()) {
                        PAsyncImage(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            data = path,
                            size = Size(context.dp2px(48), context.dp2px(48)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

        }
    }
}