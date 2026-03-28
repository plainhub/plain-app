package com.ismartcoding.plain.ui.page.audio

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.events.SleepTimerEvent
import com.ismartcoding.plain.events.CancelSleepTimerEvent
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerPage(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var timerActive by remember { mutableStateOf(false) }
    var remainingTimeMs by remember { mutableLongStateOf(0L) }
    var selectedTimeMinutes by remember { mutableIntStateOf(15) }
    var timerJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        val futureTime = TempData.audioSleepTimerFutureTime
        if (futureTime > 0) {
            timerActive = true
            val remaining = futureTime - SystemClock.elapsedRealtime()
            if (remaining > 0) remainingTimeMs = remaining
        }
        sheetState.expand()
    }

    LaunchedEffect(timerActive) {
        if (timerActive && remainingTimeMs > 0) {
            timerJob?.cancel()
            timerJob = scope.launch {
                while (true) {
                    delay(1000)
                    remainingTimeMs = maxOf(0, TempData.audioSleepTimerFutureTime - SystemClock.elapsedRealtime())
                    if (remainingTimeMs <= 0) { timerActive = false; break }
                }
            }
        }
    }

    PModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Box(modifier = Modifier.fillMaxWidth().height(500.dp).padding(bottom = 32.dp)) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
                Text(text = stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))

                if (timerActive) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().weight(1f)) {
                        SleepTimerActiveContent(remainingTimeMs, selectedTimeMinutes)
                        Spacer(modifier = Modifier.weight(1f))
                        PFilledButton(text = stringResource(R.string.cancel_timer), onClick = {
                            scope.launch {
                                withIO { TempData.audioSleepTimerFutureTime = 0 }
                                timerJob?.cancel(); timerActive = false; remainingTimeMs = 0
                                sendEvent(CancelSleepTimerEvent())
                            }
                        }, type = ButtonType.DANGER)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().weight(1f)) {
                        SleepTimerSelectionContent(selectedTimeMinutes) { selectedTimeMinutes = it }
                        Spacer(modifier = Modifier.weight(1f))
                        PFilledButton(text = stringResource(R.string.start_timer), onClick = {
                            scope.launch {
                                withIO {
                                    val durationMs = selectedTimeMinutes * 60 * 1000L
                                    TempData.audioSleepTimerFutureTime = SystemClock.elapsedRealtime() + durationMs
                                    remainingTimeMs = durationMs
                                }
                                timerActive = true
                                sendEvent(SleepTimerEvent(selectedTimeMinutes * 60 * 1000L))
                            }
                        })
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
