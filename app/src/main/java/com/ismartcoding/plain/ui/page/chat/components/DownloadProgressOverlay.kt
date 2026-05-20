package com.ismartcoding.plain.ui.page.chat.components

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.chat.download.DownloadStatus
import com.ismartcoding.plain.ui.base.PIconButton

@Composable
private fun DualProgressIndicator(
    progress: Float,
    size: Dp
) {
    val modifier = Modifier.size(size)
    // Progress circle background (track)
    CircularProgressIndicator(
        progress = { 1f },
        modifier = modifier,
        color = Color.White.copy(alpha = 0.3f),
        strokeWidth = 4.dp,
        trackColor = Color.Transparent
    )
    // Actual progress
    CircularProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier,
        color = Color.White,
        strokeWidth = 4.dp,
        trackColor = Color.Transparent
    )
}

@Composable
private fun DownloadActionButton(
    status: DownloadStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    when (status) {
        DownloadStatus.DOWNLOADING -> PIconButton(
            icon = Res.drawable.pause,
            click = onPause,
            tint = Color.White,
            contentDescription = stringResource(Res.string.pause),
            modifier = Modifier.size(24.dp)
        )

        DownloadStatus.PAUSED -> PIconButton(
            icon = Res.drawable.download,
            click = onResume,
            tint = Color.White,
            contentDescription = stringResource(Res.string.resume),
            modifier = Modifier.size(24.dp)
        )

        DownloadStatus.PENDING -> PIconButton(
            icon = Res.drawable.x,
            click = onCancel,
            tint = Color.White,
            contentDescription = stringResource(Res.string.cancel),
            modifier = Modifier.size(24.dp)
        )

        DownloadStatus.FAILED -> PIconButton(
            icon = Res.drawable.circle_alert,
            click = onResume,
            tint = Color.White,
            contentDescription = stringResource(Res.string.try_again),
            modifier = Modifier.size(24.dp)
        )

        else -> {}
    }
}

@Composable
fun DownloadProgressOverlay(
    modifier: Modifier,
    downloadProgress: Float,
    status: DownloadStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Show progress indicators for downloading and paused states
        if (status in setOf(DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED)) {
            DualProgressIndicator(
                progress = downloadProgress,
                size = size,
            )
        } else if (status == DownloadStatus.PENDING) {
            CircularProgressIndicator(
                modifier = Modifier.size(size),
                color = Color.White,
                strokeWidth = 4.dp,
                trackColor = Color.Transparent
            )
        }

        // Action button for all states
        DownloadActionButton(
            status = status,
            onPause = onPause,
            onResume = onResume,
            onCancel = onCancel
        )
    }
}
