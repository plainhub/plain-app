package com.ismartcoding.plain.ui.page.audio

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.isSPlus
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.alarmManager
import com.ismartcoding.plain.enums.AudioServiceAction
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.services.AudioPlayerService
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// Helper functions for timer intent creation
private fun makeTimerIntent(context: Context): Intent {
    val intent = Intent(context, AudioPlayerService::class.java)
    return intent.setAction(AudioServiceAction.QUIT.name)
}

private fun makeTimerPendingIntent(context: Context, flag: Int): PendingIntent {
    return PendingIntent.getService(
        context,
        0,
        makeTimerIntent(context),
        flag or PendingIntent.FLAG_IMMUTABLE
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerPage(
    onDismissRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val context = LocalContext.current
    val sharedFlow = Channel.sharedFlow

    var timerActive by remember { mutableStateOf(false) }
    var remainingTimeMs by remember { mutableLongStateOf(0L) }
    var selectedTimeMinutes by remember { mutableIntStateOf(15) }
    
    LaunchedEffect(Unit) {
        val futureTime = TempData.audioSleepTimerFutureTime
        if (futureTime > 0) {
            timerActive = true
            val remaining = futureTime - SystemClock.elapsedRealtime()
            if (remaining > 0) {
                remainingTimeMs = remaining
            }
        }

        sheetState.expand()
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    if (isSPlus()) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExact(
                                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                TempData.audioSleepTimerFutureTime,
                                makeTimerPendingIntent(context, PendingIntent.FLAG_CANCEL_CURRENT)
                            )
                        } else {
                            alarmManager.setWindow(
                                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                TempData.audioSleepTimerFutureTime,
                                1000,
                                makeTimerPendingIntent(context, PendingIntent.FLAG_CANCEL_CURRENT)
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(timerActive) {
        if (timerActive && remainingTimeMs > 0) {
            while (true) {
                delay(1000)
                remainingTimeMs = maxOf(0, TempData.audioSleepTimerFutureTime - SystemClock.elapsedRealtime())

                if (remainingTimeMs <= 0 || TempData.audioSleepTimerFutureTime <= 0) {
                    timerActive = false
                    break
                }
            }
        }
    }

    val timeOptions = listOf(5, 10, 15, 30, 45, 60, 90, 120)

    PModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(bottom = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (timerActive) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(240.dp)
                                .padding(8.dp)
                        ) {
                            val hours = TimeUnit.MILLISECONDS.toHours(remainingTimeMs)
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTimeMs) % 60
                            val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTimeMs) % 60

                            CircularProgressIndicator(
                                progress = { (remainingTimeMs / (selectedTimeMinutes * 60f * 1000f)).coerceIn(0f, 1f) },
                                modifier = Modifier.size(240.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 10.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 42.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = stringResource(R.string.remaining),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        PFilledButton(
                            text = stringResource(R.string.cancel_timer),
                            onClick = {
                                scope.launch {
                                    withIO {
                                        val previous = PendingIntent.getService(
                                            context,
                                            0,
                                            makeTimerIntent(context),
                                            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                                        )
                                        if (previous != null) {
                                            alarmManager.cancel(previous)
                                            previous.cancel()
                                        }
                                        TempData.audioSleepTimerFutureTime = 0
                                    }
                                    timerActive = false
                                    remainingTimeMs = 0
                                }
                            },
                            type = ButtonType.DANGER
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            timeOptions.take(4).forEach { minutes ->
                                TimeOptionChip(
                                    minutes = minutes,
                                    isSelected = selectedTimeMinutes == minutes,
                                    onClick = { selectedTimeMinutes = minutes }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            timeOptions.takeLast(4).forEach { minutes ->
                                TimeOptionChip(
                                    minutes = minutes,
                                    isSelected = selectedTimeMinutes == minutes,
                                    onClick = { selectedTimeMinutes = minutes }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        PFilledButton(
                            text = stringResource(R.string.start_timer),
                            onClick = {
                                scope.launch {
                                    withIO {
                                        val durationMs = selectedTimeMinutes * 60 * 1000L
                                        TempData.audioSleepTimerFutureTime = SystemClock.elapsedRealtime() + durationMs
                                        remainingTimeMs = durationMs

                                        if (isSPlus()) {
                                            if (alarmManager.canScheduleExactAlarms()) {
                                                alarmManager.setExact(
                                                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                                    TempData.audioSleepTimerFutureTime,
                                                    makeTimerPendingIntent(context, PendingIntent.FLAG_CANCEL_CURRENT)
                                                )
                                            } else {
                                                Permission.SCHEDULE_EXACT_ALARM.grant(context)
                                            }
                                        } else {
                                            alarmManager.setExact(
                                                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                                TempData.audioSleepTimerFutureTime,
                                                makeTimerPendingIntent(context, PendingIntent.FLAG_CANCEL_CURRENT)
                                            )
                                        }
                                    }
                                    timerActive = true
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeOptionChip(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .scale(scale)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 4.dp else 0.dp
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = "$minutes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "$minutes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
} 