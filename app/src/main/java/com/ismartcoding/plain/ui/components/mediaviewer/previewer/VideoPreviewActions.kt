package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.helpers.DownloadHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.PathHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PMiniButton
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.PlayerSlider
import com.ismartcoding.plain.ui.components.CastDialog
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.theme.darkMask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.seconds

data class PlaybackSpeed(
    val speed: Float,
    val label: String,
)

@Composable
fun VideoPreviewActions(
    context: Context, castViewModel: CastViewModel, m: PreviewItem,
    state: MediaPreviewerState
) {
    val videoState = state.videoState
    if (!state.showActions || videoState.enablePip || videoState.isFullscreenMode) {
        return
    }
    val scope = rememberCoroutineScope()
    CastDialog(castViewModel)

    LaunchedEffect(Unit) {
        while (true) {
            scope.launch {
                state.videoState.updateTime()
            }
            delay(1.seconds)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .alpha(state.uiAlpha.value)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            VideoButtons1(context, videoState)
            VideoButtons2(videoState, scope)
            if (castViewModel.castMode.value) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.darkMask())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        PMiniButton(label = stringResource(id = R.string.cast)) {
                            castViewModel.cast(m.path)
                        }
                        HorizontalSpace(dp = 20.dp)
                        PMiniOutlineButton(label = stringResource(id = R.string.exit_cast_mode), color = Color.LightGray) {
                            castViewModel.exitCastMode()
                        }
                    }
                }
                return
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .align(Alignment.CenterHorizontally)
                    .background(MaterialTheme.colorScheme.darkMask())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                ActionIconButton(
                    icon = R.drawable.share_2,
                    contentDescription = stringResource(R.string.share),
                ) {
                    if (m.mediaId.isNotEmpty()) {
                        ShareHelper.shareUris(context, listOf(VideoMediaStoreHelper.getItemUri(m.mediaId)))
                    } else if (m.path.isUrl()) {
                        scope.launch {
                            val tempFile = File.createTempFile("videoPreviewShare", "." + m.path.getFilenameExtension(), File(context.cacheDir, "/video_cache"))
                            DialogHelper.showLoading()
                            val r = withIO { DownloadHelper.downloadToTempAsync(m.path, tempFile) }
                            DialogHelper.hideLoading()
                            if (r.success) {
                                ShareHelper.shareFile(context, File(r.path))
                            } else {
                                DialogHelper.showMessage(r.message)
                            }
                        }
                    } else {
                        ShareHelper.shareFile(context, File(m.path))
                    }
                }
                HorizontalSpace(dp = 20.dp)
                ActionIconButton(
                    icon = R.drawable.cast,
                    contentDescription = stringResource(R.string.cast),
                ) {
                    castViewModel.showCastDialog.value = true
                }
                if (m.data !is DVideo && m.data !is DFile) {
                    HorizontalSpace(dp = 20.dp)
                    ActionIconButton(
                        icon = R.drawable.save,
                        contentDescription = stringResource(R.string.save),
                    ) {
                        scope.launch {
                            if (m.path.isUrl()) {
                                DialogHelper.showLoading()
                                val dir = PathHelper.getPlainPublicDir(Environment.DIRECTORY_MOVIES)
                                val r = withIO { DownloadHelper.downloadAsync(m.path, dir.absolutePath) }
                                DialogHelper.hideLoading()
                                if (r.success) {
                                    DialogHelper.showMessage(LocaleHelper.getStringF(R.string.video_save_to, "path", r.path))
                                } else {
                                    DialogHelper.showMessage(r.message)
                                }
                            } else {
                                val r = withIO { FileHelper.copyFileToPublicDir(m.path, Environment.DIRECTORY_MOVIES) }
                                if (r.isNotEmpty()) {
                                    DialogHelper.showMessage(LocaleHelper.getStringF(R.string.video_save_to, "path", r))
                                } else {
                                    DialogHelper.showMessage(LocaleHelper.getString(R.string.video_save_to_failed))
                                }
                            }
                        }
                    }
                }
                HorizontalSpace(dp = 20.dp)
                ActionIconButton(
                    icon = R.drawable.ellipsis,
                    contentDescription = stringResource(R.string.more_info),
                ) {
                    state.showMediaInfo = true
                }
            }
        }
    }
}

@Composable
fun VideoButtons1(context: Context, videoState: VideoState) {
    var showSpeedMenu by rememberSaveable { mutableStateOf(false) }
    val playbackSpeeds = remember {
        listOf(
            PlaybackSpeed(0.25f, "0.25x"),
            PlaybackSpeed(0.5f, "0.5x"),
            PlaybackSpeed(1f, "1x"),
            PlaybackSpeed(2f, "2x"),
            PlaybackSpeed(3f, "3x")
        )
    }

    fun setSpeed(speed: Float) {
        videoState.changeSpeed(speed)
        showSpeedMenu = false
    }

    Box(contentAlignment = Alignment.TopEnd) {
        DropdownMenu(
            expanded = showSpeedMenu,
            onDismissRequest = {
                showSpeedMenu = false
            }
        ) {
            playbackSpeeds.forEach { speed ->
                DropdownMenuItem(
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = {
                        setSpeed(speed.speed)
                    },
                    leadingIcon = {
                        RadioButton(
                            selected = videoState.speed == speed.speed,
                            onClick = {
                                setSpeed(speed.speed)
                            }
                        )
                    },
                    text = { Text(text = speed.label) }
                )
            }
        }
        IconButton(
            onClick = {
                showSpeedMenu = !showSpeedMenu
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.gauge),
                tint = Color.White,
                contentDescription = stringResource(R.string.change_playback_speed)
            )
        }
    }
    IconButton(
        onClick = {
            videoState.toggleMute()
        }
    ) {
        Icon(
            painter = painterResource(if (videoState.isMuted) R.drawable.volume_x else R.drawable.volume_2),
            tint = Color.White,
            contentDescription = stringResource(R.string.toggle_audio)
        )
    }
    if (videoState.hasPipMode(context)) {
        IconButton(
            onClick = {
                videoState.enterPipMode(context)
            },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.pip),
                tint = Color.White,
                contentDescription = stringResource(R.string.picture_in_picture),
            )
        }
    }
}

@Composable
fun VideoButtons2(videoState: VideoState, scope: CoroutineScope) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            modifier = Modifier.size(40.dp),
            onClick = {
                videoState.togglePlay()
            },
        ) {
            if (videoState.isPlaying) {
                Image(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(R.drawable.pause),
                    colorFilter = ColorFilter.tint(Color.White),
                    contentDescription = stringResource(R.string.pause),
                )
            } else {
                Image(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(R.drawable.play_arrow),
                    colorFilter = ColorFilter.tint(Color.White),
                    contentDescription = stringResource(R.string.play),
                )
            }
        }
        Text(
            modifier = Modifier.width(52.dp),
            text = videoState.currentTime.formatMinSec(),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            PlayerSlider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                progress = videoState.currentTime.toFloat() / videoState.totalTime.toFloat(),
                bufferedProgress = videoState.bufferedPercentage / 100f,
                onProgressChange = { progress ->
                    videoState.seekTo((progress * videoState.totalTime).toLong())
                }
            )
        }
        Text(
            modifier = Modifier.width(52.dp),
            text = videoState.totalTime.formatMinSec(),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        IconButton(
            modifier = Modifier.size(40.dp),
            onClick = {
                videoState.isFullscreenMode = !videoState.isFullscreenMode
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.maximize),
                tint = Color.White,
                contentDescription = stringResource(R.string.fullscreen)
            )
        }
    }
}

