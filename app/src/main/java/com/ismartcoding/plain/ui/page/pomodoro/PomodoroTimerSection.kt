package com.ismartcoding.plain.ui.page.pomodoro

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.ui.base.CircularTimer
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
internal fun PomodoroTimerSection(pomodoroVM: PomodoroViewModel, scope: CoroutineScope) {
    val context = LocalContext.current
    Box(
        modifier = Modifier.size(250.dp).pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f
                val dragVector = change.position - center
                val distance = sqrt(dragVector.x * dragVector.x + dragVector.y * dragVector.y)
                if (distance > radius - 50 && distance < radius + 50) {
                    val angle = atan2(dragVector.y, dragVector.x)
                    var normalizedAngle = (angle + PI / 2) / (2 * PI)
                    if (normalizedAngle < 0) normalizedAngle += 1
                    val totalDuration = pomodoroVM.getTotalSeconds()
                    pomodoroVM.timeLeft.intValue = (totalDuration * (1 - normalizedAngle)).toInt().coerceIn(0, totalDuration)
                    pomodoroVM.adjustJob.value?.cancel()
                    pomodoroVM.adjustJob.value = coIO {
                        delay(500)
                        Permissions.checkNotification(context, R.string.pomodoro_notification_prompt) {
                            scope.launch(Dispatchers.IO) { pomodoroVM.startSession(); sendPomodoroAction("start", pomodoroVM) }
                        }
                    }
                }
            }
        },
        contentAlignment = Alignment.Center
    ) {
        val totalDuration = pomodoroVM.getTotalSeconds()
        val progress = if (totalDuration > 0) 1f - (pomodoroVM.timeLeft.intValue.toFloat() / totalDuration.toFloat()) else 0f
        CircularTimer(progress = progress)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = pomodoroVM.formatTime(pomodoroVM.timeLeft.intValue),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface)
            VerticalSpace(dp = 16.dp)
            Text(text = stringResource(R.string.drag_to_adjust_progress),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }

    VerticalSpace(dp = 32.dp)

    if (pomodoroVM.isRunning.value) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { pomodoroVM.pauseSession(); sendPomodoroAction("pause", pomodoroVM) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(28.dp)
            ) { Text(text = stringResource(id = R.string.pause), fontWeight = FontWeight.Medium) }
            Button(onClick = { pomodoroVM.resetTimer(); sendPomodoroAction("stop", pomodoroVM) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(28.dp)
            ) { Text(text = stringResource(R.string.stop), fontWeight = FontWeight.Medium) }
        }
    } else {
        Button(onClick = {
            Permissions.checkNotification(context, R.string.pomodoro_notification_prompt) {
                scope.launch(Dispatchers.IO) { pomodoroVM.startSession(); sendPomodoroAction("start", pomodoroVM) }
            }
        }, modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(text = when {
                pomodoroVM.isPaused.value -> stringResource(id = R.string.resume)
                else -> stringResource(id = R.string.start)
            }, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}
