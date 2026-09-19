@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.components.downloads

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.download.DownloadStatus
import com.ismartcoding.plain.features.download.isTerminalDownloadStatus
import com.ismartcoding.plain.features.share.SharedFolderBatchTask
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.i18n.Res
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min

/**
 * Edge-docked overlay shown on non-share pages while batches run in the
 * background. Static ring (no animation, e-ink friendly), vertically
 * draggable; tap opens the global download list.
 */
@Composable
fun BoxScope.DownloadFloatingWidget(
    tasks: List<SharedFolderBatchTask>,
    onClick: () -> Unit,
) {
    if (tasks.isEmpty()) return
    val task = mostRelevant(tasks) ?: return
    val anyActive = tasks.any { !it.status.isTerminalDownloadStatus() }
    val attention = !anyActive && tasks.any {
        it.status == DownloadStatus.PARTIAL || it.status == DownloadStatus.FAILED
    }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .graphicsLayer { translationY = offsetY }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    offsetY = (offsetY + dragAmount).coerceIn(-520f, 520f)
                }
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        ) {
            Icon(
                painterResource(Res.drawable.download),
                contentDescription = stringResource(Res.string.downloads),
                tint = if (attention) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp).size(36.dp),
            ) {
                val fraction = task.fraction()
                val ringColor = if (attention) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.outlineVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    val inset = stroke.width / 2
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke,
                    )
                    if (anyActive) {
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = min(fraction, 1f) * 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = stroke,
                        )
                    }
                }
                Text(
                    text = if (anyActive) "${(fraction * 100).toInt()}" else "!",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (attention) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "${tasks.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
