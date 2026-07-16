package com.ismartcoding.plain.platform

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.helpers.DownloadHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.PathHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatMinSec
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PlayerSlider
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.theme.darkMask
import com.ismartcoding.plain.ui.theme.lightMask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.io.File
import kotlin.time.Duration.Companion.seconds
import androidx.compose.animation.core.SpringSpec

// ---- ImagePreviewActionOps (moved from ui/components/mediaviewer/previewer/) ----

internal suspend fun sharePreviewImage(context: Context, m: PreviewItem) {
    if (m.mediaId.isNotEmpty()) {
        ShareHelper.shareUris(context, listOf(ImageMediaStoreHelper.getItemUri(m.mediaId)))
    } else if (m.path.isUrl()) {
        val cachedPath = context.imageLoader.diskCache?.openSnapshot(m.path)?.data
        val tempFile = File.createTempFile("imagePreviewShare", "." + m.path.getFilenameExtension(), File(context.cacheDir, "/image_cache"))
        if (cachedPath != null) {
            cachedPath.toFile().copyTo(tempFile, true)
            ShareHelper.shareFile(context, tempFile, m.getMimeType().ifEmpty { "image/*" })
        } else {
            DialogHelper.showLoading()
            val r = DownloadHelper.downloadToTempAsync(m.path, tempFile)
            DialogHelper.hideLoading()
            if (r.success) {
                ShareHelper.shareFile(context, File(r.path), m.getMimeType().ifEmpty { "image/*" })
            } else {
                DialogHelper.showMessage(r.message)
            }
        }
    } else {
        ShareHelper.shareFile(context, File(m.path), m.getMimeType().ifEmpty { "image/*" })
    }
}

internal suspend fun savePreviewImage(context: Context, m: PreviewItem) {
    if (m.path.isUrl()) {
        DialogHelper.showLoading()
        val cachedPath = context.imageLoader.diskCache?.openSnapshot(m.path)?.data
        if (cachedPath != null) {
            val r = withIO { FileHelper.copyFileToPublicDir(cachedPath.toString(), Environment.DIRECTORY_PICTURES, newName = m.path.getFilenameFromPath()) }
            DialogHelper.hideLoading()
            if (r.isNotEmpty()) {
                DialogHelper.showMessage(LocaleHelper.getStringFAsync(Res.string.image_save_to, "path", r))
            } else {
                DialogHelper.showMessage(LocaleHelper.getStringAsync(Res.string.image_save_to_failed))
            }
            return
        }
        val dir = PathHelper.getPlainPublicDir(Environment.DIRECTORY_PICTURES)
        val r = DownloadHelper.downloadAsync(m.path, dir.absolutePath)
        DialogHelper.hideLoading()
        if (r.success) {
            DialogHelper.showConfirmDialog("", LocaleHelper.getStringFAsync(Res.string.image_save_to, "path", r.path))
        } else {
            DialogHelper.showMessage(r.message)
        }
    } else {
        val newName = (m.data as? DMessageFile)?.fileName?.takeIf { it.isNotEmpty() } ?: ""
        val r = withIO { FileHelper.copyFileToPublicDir(m.path, Environment.DIRECTORY_PICTURES, newName = newName) }
        if (r.isNotEmpty()) {
            DialogHelper.showMessage(LocaleHelper.getStringFAsync(Res.string.image_save_to, "path", r))
        } else {
            DialogHelper.showMessage(LocaleHelper.getStringAsync(Res.string.image_save_to_failed))
        }
    }
}

// ---- ImagePreviewActions (moved from ui/components/mediaviewer/previewer/) ----

@Composable
fun ImagePreviewActions(
    context: Context, castViewModel: CastViewModel,
    m: PreviewItem, state: MediaPreviewerState,
) {
    val scope = rememberCoroutineScope()

    CastDialog(castViewModel)

    Box(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .navigationBarsPadding()
            .alpha(state.uiAlpha.value),
    ) {
        if (!state.showActions) return
        if (castViewModel.castMode.value) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.darkMask())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                POutlinedButton(text = stringResource(Res.string.cast), buttonSize = ButtonSize.SMALL, onClick = { castViewModel.cast(m.path) })
                HorizontalSpace(dp = 20.dp)
                POutlinedButton(text = stringResource(Res.string.exit_cast_mode), buttonSize = ButtonSize.SMALL, contentColor = Color.LightGray, onClick = { castViewModel.exitCastMode() })
            }
            return
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.darkMask())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            ActionIconButton(icon = Res.drawable.share_2, contentDescription = stringResource(Res.string.share)) {
                scope.launch { sharePreviewImage(context, m) }
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(icon = Res.drawable.cast, contentDescription = stringResource(Res.string.cast)) {
                castViewModel.showCastDialog.value = true
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(icon = Res.drawable.rotate_cw_square, contentDescription = stringResource(Res.string.rotate)) {
                scope.launch {
                    state.viewerContainerState?.viewerState?.let {
                        it.rotation.animateTo(it.rotation.value + 90, SpringSpec())
                    }
                }
            }
            if (m.data !is DImage && m.data !is DFile) {
                HorizontalSpace(dp = 20.dp)
                ActionIconButton(icon = Res.drawable.save, contentDescription = stringResource(Res.string.save)) {
                    scope.launch { savePreviewImage(context, m) }
                }
            }
            HorizontalSpace(dp = 20.dp)
            ActionIconButton(icon = Res.drawable.ellipsis, contentDescription = stringResource(Res.string.more_info)) {
                state.showMediaInfo = true
            }
        }
    }
}

@Composable
fun ActionIconButton(icon: DrawableResource, contentDescription: String, click: () -> Unit) {
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.lightMask())
            .clickable { click() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}

// ---- VideoPreviewButtons (moved from ui/components/mediaviewer/previewer/) ----

data class PlaybackSpeed(val speed: Float, val label: String)

@Composable
fun VideoButtons1(context: Context, videoState: VideoState) {
    var showSpeedMenu by rememberSaveable { mutableStateOf(false) }
    val playbackSpeeds = remember {
        listOf(PlaybackSpeed(0.25f, "0.25x"), PlaybackSpeed(0.5f, "0.5x"), PlaybackSpeed(1f, "1x"), PlaybackSpeed(2f, "2x"), PlaybackSpeed(3f, "3x"))
    }
    fun setSpeed(speed: Float) { videoState.changeSpeed(speed); showSpeedMenu = false }

    Box(contentAlignment = Alignment.TopEnd) {
        DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
            playbackSpeeds.forEach { speed ->
                DropdownMenuItem(modifier = Modifier.padding(end = 16.dp), onClick = { setSpeed(speed.speed) },
                    leadingIcon = { RadioButton(selected = videoState.speed == speed.speed, onClick = { setSpeed(speed.speed) }) },
                    text = { Text(text = speed.label) })
            }
        }
        IconButton(onClick = { showSpeedMenu = !showSpeedMenu }) {
            Icon(painter = painterResource(Res.drawable.gauge), tint = Color.White, contentDescription = stringResource(Res.string.change_playback_speed))
        }
    }
    IconButton(onClick = { videoState.toggleMute() }) {
        Icon(painter = painterResource(if (videoState.isMuted) Res.drawable.volume_x else Res.drawable.volume_2), tint = Color.White, contentDescription = stringResource(Res.string.toggle_audio))
    }
    if (hasPipMode(context)) {
        IconButton(onClick = { enterPipMode(videoState) }) {
            Icon(painter = painterResource(Res.drawable.pip), tint = Color.White, contentDescription = stringResource(Res.string.picture_in_picture))
        }
    }
}

// ---- VideoPreviewActions (moved from ui/components/mediaviewer/previewer/) ----

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
                        POutlinedButton(text = stringResource(Res.string.cast), buttonSize = ButtonSize.SMALL, onClick = { castViewModel.cast(m.path) })
                        HorizontalSpace(dp = 20.dp)
                        POutlinedButton(text = stringResource(Res.string.exit_cast_mode), buttonSize = ButtonSize.SMALL, contentColor = Color.LightGray, onClick = { castViewModel.exitCastMode() })
                    }
                }
                return
            }
            Row(modifier = Modifier.clip(RoundedCornerShape(50)).align(Alignment.CenterHorizontally).background(MaterialTheme.colorScheme.darkMask()).padding(horizontal = 20.dp, vertical = 8.dp)) {
                ActionIconButton(icon = Res.drawable.share_2, contentDescription = stringResource(Res.string.share)) {
                    if (m.mediaId.isNotEmpty()) { ShareHelper.shareUris(context, listOf(VideoMediaStoreHelper.getItemUri(m.mediaId))) }
                    else if (m.path.isUrl()) { scope.launch { val tempFile = File.createTempFile("videoPreviewShare", "." + m.path.getFilenameExtension(), File(context.cacheDir, "/video_cache")); DialogHelper.showLoading(); val r = DownloadHelper.downloadToTempAsync(m.path, tempFile); DialogHelper.hideLoading(); if (r.success) ShareHelper.shareFile(context, File(r.path), m.getMimeType().ifEmpty { "video/*" }) else DialogHelper.showMessage(r.message) } }
                    else ShareHelper.shareFile(context, File(m.path), m.getMimeType().ifEmpty { "video/*" })
                }
                HorizontalSpace(dp = 20.dp)
                ActionIconButton(icon = Res.drawable.cast, contentDescription = stringResource(Res.string.cast)) { castViewModel.showCastDialog.value = true }
                if (m.data !is DVideo && m.data !is DFile) {
                    HorizontalSpace(dp = 20.dp)
                    ActionIconButton(icon = Res.drawable.save, contentDescription = stringResource(Res.string.save)) {
                        scope.launch {
                            if (m.path.isUrl()) { DialogHelper.showLoading(); val dir = PathHelper.getPlainPublicDir(Environment.DIRECTORY_MOVIES); val r = DownloadHelper.downloadAsync(m.path, dir.absolutePath); DialogHelper.hideLoading(); if (r.success) DialogHelper.showMessage(LocaleHelper.getStringFAsync(Res.string.video_save_to, "path", r.path)) else DialogHelper.showMessage(r.message) }
                            else { val newName = (m.data as? DMessageFile)?.fileName?.takeIf { it.isNotEmpty() } ?: ""; val r = withIO { FileHelper.copyFileToPublicDir(m.path, Environment.DIRECTORY_MOVIES, newName = newName) }; if (r.isNotEmpty()) DialogHelper.showMessage(LocaleHelper.getStringFAsync(Res.string.video_save_to, "path", r)) else DialogHelper.showMessage(LocaleHelper.getStringAsync(Res.string.video_save_to_failed)) }
                        }
                    }
                }
                HorizontalSpace(dp = 20.dp)
                ActionIconButton(icon = Res.drawable.ellipsis, contentDescription = stringResource(Res.string.more_info)) { state.showMediaInfo = true }
            }
        }
    }
}

@Composable
fun VideoButtons2(videoState: VideoState, scope: CoroutineScope) {
    val sliderProgress = if (videoState.totalTime <= 0L) 0f else (videoState.currentTime.toFloat() / videoState.totalTime.toFloat()).coerceIn(0f, 1f)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(modifier = Modifier.size(40.dp), onClick = { videoState.togglePlay() }) {
            Image(modifier = Modifier.size(32.dp), painter = painterResource(if (videoState.isPlaying) Res.drawable.pause else Res.drawable.play_arrow), colorFilter = ColorFilter.tint(Color.White), contentDescription = stringResource(if (videoState.isPlaying) Res.string.pause else Res.string.play))
        }
        Text(modifier = Modifier.width(52.dp), text = videoState.currentTime.formatMinSec(), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
        Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            PlayerSlider(modifier = Modifier.fillMaxWidth().height(20.dp), progress = sliderProgress, bufferedProgress = videoState.bufferedPercentage / 100f, onProgressChange = { videoState.seekTo((it * videoState.totalTime).toLong()) })
        }
        Text(modifier = Modifier.width(52.dp), text = videoState.totalTime.formatMinSec(), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
        IconButton(modifier = Modifier.size(40.dp), onClick = { videoState.isFullscreenMode = !videoState.isFullscreenMode }) {
            Icon(painter = painterResource(Res.drawable.maximize), tint = Color.White, contentDescription = stringResource(Res.string.fullscreen))
        }
    }
}
