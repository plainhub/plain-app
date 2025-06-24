package com.ismartcoding.plain.ui.page.pomodoro

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PomodoroActionData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.preferences.PomodoroSettingsPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.CircularTimer
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroPage(
    navController: NavHostController,
    pomodoroVM: PomodoroViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    if (pomodoroVM.showSettings.value) {
        PomodoroSettingsDialog(
            settings = pomodoroVM.settings.value,
            onSettingsChange = { newSettings ->
                scope.launch {
                    pomodoroVM.settings.value = newSettings
                    withIO { PomodoroSettingsPreference.putAsync(context, newSettings) }
                    if (!pomodoroVM.isRunning.value) {
                        pomodoroVM.updateTimeForCurrentState()
                    }
                    sendEvent(WebSocketEvent(EventType.POMODORO_SETTINGS_UPDATE, JsonHelper.jsonEncode(newSettings)))
                }
            },
            onDismiss = { pomodoroVM.showSettings.value = false }
        )
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = "",
                actions = {
                    PIconButton(
                        icon = R.drawable.settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        pomodoroVM.showSettings.value = true
                    }
                },
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(top = paddingValues.calculateTopPadding())
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = pomodoroVM.currentState.value.getText(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    VerticalSpace(dp = 8.dp)

                    Text(
                        text = stringResource(R.string.round_counter, pomodoroVM.currentRound.intValue, pomodoroVM.settings.value.pomodorosBeforeLongBreak),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    VerticalSpace(dp = 32.dp)
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.width / 2f
                                    val dragVector = change.position - center
                                    val distance = sqrt(dragVector.x * dragVector.x + dragVector.y * dragVector.y)

                                    // Only respond to drags near the circle edge
                                    if (distance > radius - 50 && distance < radius + 50) {
                                        val angle = atan2(dragVector.y, dragVector.x)
                                        var normalizedAngle = (angle + PI / 2) / (2 * PI)
                                        if (normalizedAngle < 0) normalizedAngle += 1

                                        val totalDuration = pomodoroVM.getTotalSeconds()
                                        val newTimeLeft = (totalDuration * (1 - normalizedAngle)).toInt().coerceIn(0, totalDuration)
                                        pomodoroVM.timeLeft.intValue = newTimeLeft
                                        pomodoroVM.adjustJob.value?.cancel() // Cancel any previous adjustment
                                        pomodoroVM.adjustJob.value = coIO {
                                            delay(500) // Debounce to prevent too frequent updates
                                            Permissions.checkNotification(
                                                context,
                                                R.string.pomodoro_notification_prompt
                                            ) {
                                                scope.launch(Dispatchers.IO) {
                                                    pomodoroVM.startSession()
                                                    sendEvent(
                                                        WebSocketEvent(
                                                            EventType.POMODORO_ACTION, JsonHelper.jsonEncode(
                                                                PomodoroActionData(
                                                                    "start",
                                                                    pomodoroVM.timeLeft.intValue,
                                                                    pomodoroVM.settings.value.getTotalSeconds(pomodoroVM.currentState.value),
                                                                    pomodoroVM.completedCount.intValue,
                                                                    pomodoroVM.currentRound.value,
                                                                    pomodoroVM.currentState.value
                                                                )
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val totalDuration = pomodoroVM.getTotalSeconds()
                        val progress = if (totalDuration > 0) {
                            1f - (pomodoroVM.timeLeft.intValue.toFloat() / totalDuration.toFloat())
                        } else {
                            0f
                        }

                        CircularTimer(progress = progress)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = pomodoroVM.formatTime(pomodoroVM.timeLeft.intValue),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            VerticalSpace(dp = 16.dp)

                            Text(
                                text = stringResource(R.string.drag_to_adjust_progress),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    VerticalSpace(dp = 32.dp)

                    if (pomodoroVM.isRunning.value) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    pomodoroVM.pauseSession()
                                    sendEvent(
                                        WebSocketEvent(
                                            EventType.POMODORO_ACTION, JsonHelper.jsonEncode(
                                                PomodoroActionData(
                                                    "pause",
                                                    pomodoroVM.timeLeft.intValue,
                                                    pomodoroVM.settings.value.getTotalSeconds(pomodoroVM.currentState.value),
                                                    pomodoroVM.completedCount.intValue,
                                                    pomodoroVM.currentRound.value,
                                                    pomodoroVM.currentState.value
                                                )
                                            )
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.pause),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    pomodoroVM.resetTimer()
                                    sendEvent(
                                        WebSocketEvent(
                                            EventType.POMODORO_ACTION, JsonHelper.jsonEncode(
                                                PomodoroActionData(
                                                    "stop",
                                                    pomodoroVM.timeLeft.intValue,
                                                    pomodoroVM.settings.value.getTotalSeconds(pomodoroVM.currentState.value),
                                                    pomodoroVM.completedCount.intValue,
                                                    pomodoroVM.currentRound.value,
                                                    pomodoroVM.currentState.value
                                                )
                                            )
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.stop),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                Permissions.checkNotification(
                                    context,
                                    R.string.pomodoro_notification_prompt
                                ) {
                                    scope.launch(Dispatchers.IO) {
                                        pomodoroVM.startSession()
                                        sendEvent(
                                            WebSocketEvent(
                                                EventType.POMODORO_ACTION, JsonHelper.jsonEncode(
                                                    PomodoroActionData(
                                                        "start",
                                                        pomodoroVM.timeLeft.intValue,
                                                        pomodoroVM.settings.value.getTotalSeconds(pomodoroVM.currentState.value),
                                                        pomodoroVM.completedCount.intValue,
                                                        pomodoroVM.currentRound.value,
                                                        pomodoroVM.currentState.value
                                                    )
                                                )
                                            )
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text(
                                text = when {
                                    pomodoroVM.isPaused.value -> stringResource(id = R.string.resume)
                                    !pomodoroVM.isRunning.value -> stringResource(id = R.string.start)
                                    else -> stringResource(id = R.string.start)
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    VerticalSpace(dp = 16.dp)
                }
                item {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.today_completed),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        VerticalSpace(dp = 16.dp)

                        // Pomodoro tomato icons
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val completedCount = pomodoroVM.completedCount.intValue
                            val displayCount = if (completedCount <= 4) 4 else completedCount

                            repeat(displayCount) { index ->
                                Text(
                                    text = "🍅",
                                    fontSize = 24.sp,
                                    color = if (index < completedCount) {
                                        Color.Unspecified // Full color for completed
                                    } else {
                                        Color.Gray.copy(alpha = 0.3f) // Faded for incomplete
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        VerticalSpace(dp = 16.dp)

                        Text(
                            text = pluralStringResource(
                                R.plurals.n_pomodoros,
                                pomodoroVM.completedCount.intValue,
                                pomodoroVM.completedCount.intValue
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    BottomSpace()
                }
            }
        }
    )
}