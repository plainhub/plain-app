package com.ismartcoding.plain.ui.page.pomodoro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsDialog(
    settings: DPomodoroSettings,
    onSettingsChange: (DPomodoroSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var workDuration by remember { mutableStateOf(settings.workDuration.toString()) }
    var shortBreakDuration by remember { mutableStateOf(settings.shortBreakDuration.toString()) }
    var longBreakDuration by remember { mutableStateOf(settings.longBreakDuration.toString()) }
    var pomodorosBeforeLongBreak by remember { mutableStateOf(settings.pomodorosBeforeLongBreak.toString()) }
    var showNotification by remember { mutableStateOf(settings.showNotification) }
    var playSoundOnComplete by remember { mutableStateOf(settings.playSoundOnComplete) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = workDuration,
                    onValueChange = { workDuration = it },
                    label = { Text(stringResource(R.string.work_duration)) },
                    modifier = Modifier.fillMaxWidth()
                )

                VerticalSpace(dp = 8.dp)

                OutlinedTextField(
                    value = shortBreakDuration,
                    onValueChange = { shortBreakDuration = it },
                    label = { Text(stringResource(R.string.short_break_duration)) },
                    modifier = Modifier.fillMaxWidth()
                )

                VerticalSpace(dp = 8.dp)

                OutlinedTextField(
                    value = longBreakDuration,
                    onValueChange = { longBreakDuration = it },
                    label = { Text(stringResource(R.string.long_break_duration)) },
                    modifier = Modifier.fillMaxWidth()
                )

                VerticalSpace(dp = 8.dp)

                OutlinedTextField(
                    value = pomodorosBeforeLongBreak,
                    onValueChange = { pomodorosBeforeLongBreak = it },
                    label = { Text(stringResource(R.string.pomodoros_before_long_break)) },
                    modifier = Modifier.fillMaxWidth()
                )

                VerticalSpace(dp = 16.dp)

                PDialogListItem(
                    title = stringResource(R.string.show_notification),
                ) {
                    PSwitch(
                        activated = showNotification,
                    ) {
                        showNotification = it
                    }
                }

                VerticalSpace(dp = 8.dp)

                PDialogListItem(
                    title = stringResource(R.string.play_sound_on_complete),
                ) {
                    PSwitch(
                        activated = playSoundOnComplete,
                    ) {
                        playSoundOnComplete = it
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newSettings = DPomodoroSettings(
                        workDuration = workDuration.toIntOrNull() ?: 25,
                        shortBreakDuration = shortBreakDuration.toIntOrNull() ?: 5,
                        longBreakDuration = longBreakDuration.toIntOrNull() ?: 15,
                        pomodorosBeforeLongBreak = pomodorosBeforeLongBreak.toIntOrNull() ?: 4,
                        showNotification = showNotification,
                        playSoundOnComplete = playSoundOnComplete
                    )
                    onSettingsChange(newSettings)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}