package com.ismartcoding.plain.ui.pomodoro

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.ui.components.ButtonType
import com.ismartcoding.plain.ui.base.CircularTimer
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
internal fun PomodoroTimerSection(
    pomodoroVM: PomodoroViewModel,
    scope: CoroutineScope,
    onCheckNotificationPermission: (() -> Unit) -> Unit,
) {
    var dragSeconds by remember { mutableStateOf<Int?>(null) }
    var pendingSeconds by remember { mutableStateOf<Int?>(null) }
    val displayedSeconds = dragSeconds ?: pendingSeconds ?: pomodoroVM.timeLeft.intValue

    Box(
        modifier = Modifier
            .size(250.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragCancel = { dragSeconds = null },
                    onDragEnd = {
                        val seconds = dragSeconds
                        dragSeconds = null
                        if (seconds != null) {
                            onCheckNotificationPermission {
                                scope.launchSafe(onDone = { pendingSeconds = null }) {
                                    pendingSeconds = seconds
                                    pomodoroVM.startSessionAsync(seconds)
                                }
                            }
                        }
                    },
                ) { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f
                    val dragVector = change.position - center
                    val distance = sqrt(dragVector.x * dragVector.x + dragVector.y * dragVector.y)
                    if (pendingSeconds == null && distance > radius - 50 && distance < radius + 50) {
                        val angle = atan2(dragVector.y, dragVector.x)
                        var normalizedAngle = (angle + PI / 2) / (2 * PI)
                        if (normalizedAngle < 0) normalizedAngle += 1
                        val totalDurationSec = pomodoroVM.getTotalSeconds()
                        dragSeconds = (totalDurationSec * (1 - normalizedAngle)).toInt().coerceIn(1, totalDurationSec)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val totalDurationSec = pomodoroVM.getTotalSeconds()
        val progress = if (totalDurationSec > 0) 1f - (displayedSeconds.toFloat() / totalDurationSec.toFloat()) else 0f
        CircularTimer(progress = progress)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = pomodoroVM.formatTime(displayedSeconds),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            VerticalSpace(dp = 16.dp)
            Text(
                text = stringResource(Res.string.drag_to_adjust_progress),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }

    VerticalSpace(dp = 32.dp)

    if (pomodoroVM.isRunning.value) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            PFilledButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.pause),
                onClick = { pomodoroVM.pauseSession() },
            )
            PFilledButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.stop),
                type = ButtonType.DANGER,
                onClick = {
                    pomodoroVM.resetTimer()
                },
            )
        }
    } else {
        PFilledButton(
            modifier = Modifier.fillMaxWidth(),
            text = when {
                pomodoroVM.isPaused.value -> stringResource(Res.string.resume)
                else -> stringResource(Res.string.start)
            },
            onClick = {
                onCheckNotificationPermission {
                    scope.launch(Dispatchers.Default) {
                        pomodoroVM.startSession()
                    }
                }
            },
        )
    }
}
