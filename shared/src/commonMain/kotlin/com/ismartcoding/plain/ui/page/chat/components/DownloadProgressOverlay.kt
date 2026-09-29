package com.ismartcoding.plain.ui.page.chat.components

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.chat.download.DownloadQueue
import com.ismartcoding.plain.features.download.DownloadStatus
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.circle_alert as ui_drawable_circle_alert
import com.ismartcoding.plain.ui.resources.download as ui_drawable_download
import com.ismartcoding.plain.ui.resources.pause as ui_drawable_pause
import com.ismartcoding.plain.ui.resources.x as ui_drawable_x
import com.ismartcoding.plain.i18n.download
import com.ismartcoding.plain.i18n.pause

@Composable
private fun DualProgressIndicator(
    progress: Float,
    size: Dp
) {
    val modifier = Modifier.size(size)
    CircularProgressIndicator(
        progress = { 1f },
        modifier = modifier,
        color = Color.White.copy(alpha = 0.3f),
        strokeWidth = 3.dp,
        trackColor = Color.Transparent
    )
    CircularProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier,
        color = Color.White,
        strokeWidth = 3.dp,
        trackColor = Color.Transparent
    )
}

@Composable
private fun DownloadActionButton(
    taskId: String,
    status: DownloadStatus,
) {
    when (status) {
        DownloadStatus.DOWNLOADING -> PIconButton(
            icon = UiRes.drawable.ui_drawable_pause,
            click = { DownloadQueue.pauseDownload(taskId) },
            tint = Color.White,
            contentDescription = stringResource(Res.string.pause),
            modifier = Modifier.size(24.dp)
        )

        DownloadStatus.PAUSED -> PIconButton(
            icon = UiRes.drawable.ui_drawable_download,
            click = { DownloadQueue.resumeDownload(taskId) },
            tint = Color.White,
            contentDescription = stringResource(Res.string.resume),
            modifier = Modifier.size(24.dp)
        )

        DownloadStatus.PENDING -> PIconButton(
            icon = UiRes.drawable.ui_drawable_x,
            click = { DownloadQueue.removeDownload(taskId) },
            tint = Color.White,
            contentDescription = stringResource(Res.string.cancel),
            modifier = Modifier.size(24.dp)
        )

        DownloadStatus.FAILED -> PIconButton(
            icon = UiRes.drawable.ui_drawable_circle_alert,
            click = { DownloadQueue.retryDownload(taskId) },
            tint = Color.White,
            contentDescription = stringResource(Res.string.try_again),
            modifier = Modifier.size(24.dp)
        )

        else -> {}
    }
}

@Composable
fun DownloadProgressOverlay(
    taskId: String,
    status: DownloadStatus,
    modifier: Modifier,
    downloadProgress: Float,
    size: Dp = 48.dp,
    cornerRadius: Dp = 6.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        if (status in setOf(DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED)) {
            DualProgressIndicator(
                progress = downloadProgress,
                size = size,
            )
        } else if (status == DownloadStatus.PENDING) {
            CircularProgressIndicator(
                modifier = Modifier.size(size),
                color = Color.White,
                strokeWidth = 3.dp,
                trackColor = Color.Transparent
            )
        }

        DownloadActionButton(
            taskId = taskId,
            status = status,
        )
    }
}
