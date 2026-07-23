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
import com.ismartcoding.plain.chat.download.DownloadStatus
import com.ismartcoding.plain.ui.base.PIconButton

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
            icon = Res.drawable.pause,
            click = { DownloadQueue.pauseDownload(taskId) },
            tint = Color.White,
            contentDescription = stringResource(Res.string.pause),
            modifier = Modifier.size(24.dp)
        )

        DownloadStatus.PAUSED -> PIconButton(
            icon = Res.drawable.download,
            click = { DownloadQueue.resumeDownload(taskId) },
            tint = Color.White,
            contentDescription = stringResource(Res.string.resume),
            modifier = Modifier.size(24.dp)
        )

        DownloadStatus.PENDING -> PIconButton(
            icon = Res.drawable.x,
            click = { DownloadQueue.removeDownload(taskId) },
            tint = Color.White,
            contentDescription = stringResource(Res.string.cancel),
            modifier = Modifier.size(24.dp)
        )

        DownloadStatus.FAILED -> PIconButton(
            icon = Res.drawable.circle_alert,
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
