package com.ismartcoding.plain.ui.page.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.preferences.PomodoroSettingsPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroPage(navController: NavHostController, pomodoroVM: PomodoroViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (pomodoroVM.showSettings.value) {
        PomodoroSettingsDialog(settings = pomodoroVM.settings.value, onSettingsChange = { newSettings ->
            scope.launch {
                pomodoroVM.settings.value = newSettings
                withIO { PomodoroSettingsPreference.putAsync(context, newSettings) }
                if (!pomodoroVM.isRunning.value) pomodoroVM.updateTimeForCurrentState()
                sendEvent(WebSocketEvent(EventType.POMODORO_SETTINGS_UPDATE, JsonHelper.jsonEncode(newSettings)))
            }
        }, onDismiss = { pomodoroVM.showSettings.value = false })
    }

    PScaffold(topBar = {
        PTopAppBar(navController = navController, title = "", actions = {
            PIconButton(icon = R.drawable.settings, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurface) { pomodoroVM.showSettings.value = true }
        })
    }) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding()).fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(text = pomodoroVM.currentState.value.getText(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                }
                VerticalSpace(dp = 8.dp)
                Text(text = stringResource(R.string.round_counter, pomodoroVM.currentRound.intValue, pomodoroVM.settings.value.pomodorosBeforeLongBreak),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                VerticalSpace(dp = 32.dp)
            }
            item {
                PomodoroTimerSection(pomodoroVM, scope)
                VerticalSpace(dp = 16.dp)
            }
            item {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(R.string.today_completed), style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    VerticalSpace(dp = 16.dp)
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        val completedCount = pomodoroVM.completedCount.intValue
                        val displayCount = if (completedCount <= 4) 4 else completedCount
                        repeat(displayCount) { index ->
                            Text(text = "\ud83c\udf45", fontSize = 24.sp,
                                color = if (index < completedCount) Color.Unspecified else Color.Gray.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    VerticalSpace(dp = 16.dp)
                    Text(text = pluralStringResource(R.plurals.n_pomodoros, pomodoroVM.completedCount.intValue, pomodoroVM.completedCount.intValue),
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                BottomSpace(paddingValues)
            }
        }
    }
}
