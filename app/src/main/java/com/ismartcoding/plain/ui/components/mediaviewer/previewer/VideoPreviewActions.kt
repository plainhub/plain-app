package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.formatMinSec
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.helpers.DownloadHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.PathHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PlayerSlider
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.theme.darkMask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.seconds
import androidx.compose.foundation.Image

@Composable
fun VideoPreviewActions(context: Context, castViewModel: CastViewModel, m: PreviewItem, state: MediaPreviewerState) {
    val videoState = state.videoState
    if (!state.showActions || videoState.enablePip || videoState.isFullscreenMode) return
    val scope = rememberCoroutineScope()
    CastDialog(castViewModel)
    LaunchedEffect(Unit) { while (true) { scope.launch { state.videoState.updateTime() }; delay(1.seconds) } }

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp).navigationBarsPadding().alpha(state.uiAlpha.value)) {
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
            VideoButtons1(context, videoState)
            VideoButtons2(videoState, scope)
            if (castViewModel.castMode.value) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.align(Alignment.BottomCenter).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.darkMask()).padding(horizontal = 20.dp, vertical = 8.dp)) {
                        POutlinedButton(text = stringResource(id = R.string.cast), small = true, onClick = { castViewModel.cast(m.path) })
                        HorizontalSpace(dp = 20.dp)
                        POutlinedButton(text = stringResource(id = R.string.exit_cast_mode), small = true, contentColor = Color.LightGray, onClick = { castViewModel.exitCastMode() })
                    }
                }
                return
            }
            Row(modifier = Modifier.clip(RoundedCornerShape(50)).align(Alignment.CenterHorizontally).background(MaterialTheme.colorScheme.darkMask()).padding(horizontal = 20.dp, vertical = 8.dp)) {
                ActionIconButton(icon = R.drawable.share_2, contentDescription = stringResource(R.string.share)) {
                    if (m.mediaId.isNotEmpty()) { ShareHelper.shareUris(context, listOf(VideoMediaStoreHelper.getItemUri(m.mediaId))) }
                    else if (m.path.isUrl()) { scope.launch { val tempFile = File.createTempFile("videoPreviewShare", "." + m.path.getFilenameExtension(), File(context.cacheDir, "/video_cache")); DialogHelper.showLoading(); val r = withIO { DownloadHelper.downloadToTempAsync(m.path, tempFile) }; DialogHelper.hideLoading(); if (r.success) ShareHelper.shareFile(context, File(r.path), m.getMimeType().ifEmpty { "video/*" }) else DialogHelper.showMessage(r.message) } }
                    else ShareHelper.shareFile(context, File(m.path), m.getMimeType().ifEmpty { "video/*" })
                }
                HorizontalSpace(dp = 20.dp)
                ActionIconButton(icon = R.drawable.cast, contentDescription = stringResource(R.string.cast)) { castViewModel.showCastDialog.value = true }
                if (m.data !is DVideo && m.data !is DFile) {
                    HorizontalSpace(dp = 20.dp)
                    ActionIconButton(icon = R.drawable.save, contentDescription = stringResource(R.string.save)) {
                        scope.launch {
                            if (m.path.isUrl()) { DialogHelper.showLoading(); val dir = PathHelper.getPlainPublicDir(Environment.DIRECTORY_MOVIES); val r = withIO { DownloadHelper.downloadAsync(m.path, dir.absolutePath) }; DialogHelper.hideLoading(); if (r.success) DialogHelper.showMessage(LocaleHelper.getStringF(R.string.video_save_to, "path", r.path)) else DialogHelper.showMessage(r.message) }
                            else { val newName = (m.data as? DMessageFile)?.fileName?.takeIf { it.isNotEmpty() } ?: ""; val r = withIO { FileHelper.copyFileToPublicDir(m.path, Environment.DIRECTORY_MOVIES, newName = newName) }; if (r.isNotEmpty()) DialogHelper.showMessage(LocaleHelper.getStringF(R.string.video_save_to, "path", r)) else DialogHelper.showMessage(LocaleHelper.getString(R.string.video_save_to_failed)) }
                        }
                    }
                }
                HorizontalSpace(dp = 20.dp)
                ActionIconButton(icon = R.drawable.ellipsis, contentDescription = stringResource(R.string.more_info)) { state.showMediaInfo = true }
            }
        }
    }
}

@Composable
fun VideoButtons2(videoState: VideoState, scope: CoroutineScope) {
    val sliderProgress = if (videoState.totalTime <= 0L) 0f else (videoState.currentTime.toFloat() / videoState.totalTime.toFloat()).coerceIn(0f, 1f)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(modifier = Modifier.size(40.dp), onClick = { videoState.togglePlay() }) {
            Image(modifier = Modifier.size(32.dp), painter = painterResource(if (videoState.isPlaying) R.drawable.pause else R.drawable.play_arrow), colorFilter = ColorFilter.tint(Color.White), contentDescription = stringResource(if (videoState.isPlaying) R.string.pause else R.string.play))
        }
        Text(modifier = Modifier.width(52.dp), text = videoState.currentTime.formatMinSec(), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
        Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            PlayerSlider(modifier = Modifier.fillMaxWidth().height(20.dp), progress = sliderProgress, bufferedProgress = videoState.bufferedPercentage / 100f, onProgressChange = { videoState.seekTo((it * videoState.totalTime).toLong()) })
        }
        Text(modifier = Modifier.width(52.dp), text = videoState.totalTime.formatMinSec(), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
        IconButton(modifier = Modifier.size(40.dp), onClick = { videoState.isFullscreenMode = !videoState.isFullscreenMode }) {
            Icon(painter = painterResource(R.drawable.maximize), tint = Color.White, contentDescription = stringResource(R.string.fullscreen))
        }
    }
}
