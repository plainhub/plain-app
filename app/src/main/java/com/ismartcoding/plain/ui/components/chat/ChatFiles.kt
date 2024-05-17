package com.ismartcoding.plain.ui.components.chat

import android.content.Context
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isPdfFile
import com.ismartcoding.lib.extensions.isTextFile
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.enums.TextFileType
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.audio.AudioPlayerDialog
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformImageView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.extensions.navigateOtherFile
import com.ismartcoding.plain.ui.extensions.navigatePdf
import com.ismartcoding.plain.ui.extensions.navigateTextFile
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.VChat
import java.io.File

@Composable
fun ChatFiles(
    context: Context,
    navController: NavHostController,
    m: VChat,
    previewerState: MediaPreviewerState,
) {
    val fileItems = (m.value as DMessageFiles).items
    val context = LocalContext.current as ComponentActivity
    val keyboardController = LocalSoftwareKeyboardController.current
    Column {
        fileItems.forEachIndexed { index, item ->
            val itemState = rememberTransformItemState()
            val path = item.uri.getFinalPath(context)
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (path.isImageFast() || path.isVideoFast()) {
                            coMain {
                                keyboardController?.hide()
                                withIO {
                                    MediaPreviewData.setDataAsync(
                                        context, itemState, fileItems
                                            .filter { it.uri.isVideoFast() || it.uri.isImageFast() }, item
                                    )
                                }
                                previewerState.openTransform(
                                    index = MediaPreviewData.items.indexOfFirst { it.id == item.id },
                                    itemState = itemState,
                                )
                            }
                        } else if (path.isAudioFast()) {
                            AudioPlayerDialog().show()
                            Permissions.checkNotification(context, R.string.audio_notification_prompt) {
                                AudioPlayer.play(context, DPlaylistAudio.fromPath(context, path))
                            }
                        } else if (path.isTextFile()) {
                            navController.navigateTextFile(path, mediaId = "", type = TextFileType.CHAT)
                        } else if (path.isPdfFile()) {
                            navController.navigatePdf(File(path).toUri())
                        } else {
                            navController.navigateOtherFile(path)
                        }
                    },
            ) {
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = if (index == 0) 16.dp else 6.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, end = 8.dp),
                            text = path.getFilenameFromPath(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                        )
                        Text(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            text = FormatHelper.formatBytes(item.size) + if (item.duration > 0) " / ${FormatHelper.formatDuration(item.duration)}" else "",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                        )
                    }
                    if (path.isImageFast() || path.isVideoFast()) {
                        TransformImageView(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            path = path,
                            key = item.id,
                            itemState = itemState,
                            previewerState = previewerState,
                        )
                    } else {
                        AsyncImage(
                            model = AppHelper.getFileIconPath(path.getFilenameExtension()),
                            modifier = Modifier
                                .size(48.dp),
                            alignment = Alignment.Center,
                            contentDescription = path,
                        )
                    }
                }
            }
        }
    }
}
