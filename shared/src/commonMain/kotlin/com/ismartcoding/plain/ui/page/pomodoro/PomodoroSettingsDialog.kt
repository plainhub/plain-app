package com.ismartcoding.plain.ui.page.pomodoro

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.platform.copyPickedFileToAppStorage
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.dialogSheetBackground
import com.ismartcoding.plain.ui.base.PListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsDialog(
    settings: DPomodoroSettings, onSettingsChange: (DPomodoroSettings) -> Unit, onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var workDurationMin by remember { mutableStateOf(settings.workDurationMin.toString()) }
    var shortBreakDurationMin by remember { mutableStateOf(settings.shortBreakDurationMin.toString()) }
    var longBreakDurationMin by remember { mutableStateOf(settings.longBreakDurationMin.toString()) }
    var pomodorosBeforeLongBreak by remember { mutableStateOf(settings.pomodorosBeforeLongBreak.toString()) }
    var showNotification by remember { mutableStateOf(settings.showNotification) }
    var playSoundOnComplete by remember { mutableStateOf(settings.playSoundOnComplete) }
    var soundPath by remember { mutableStateOf(settings.soundPath) }
    var originalFileName by remember { mutableStateOf(settings.originalSoundName) }

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            if (event is PickFileResultEvent && event.tag == PickFileTag.POMODORO && event.uris.isNotEmpty()) {
                scope.launch {
                    try {
                        val displayName = copyPickedFileToAppStorage(event.uris.first(), "audio/pomodoro_sound.mp3")
                        if (displayName != null) {
                            originalFileName = displayName
                            soundPath = "app://audio/pomodoro_sound.mp3"
                        }
                    } catch (e: Exception) { LogCat.e("Failed to copy pomodoro sound file: ${e.message}") }
                }
            }
        }
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.settings), style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(value = workDurationMin, onValueChange = { workDurationMin = it },
                        label = { Text(stringResource(Res.string.work_duration)) }, modifier = Modifier.fillMaxWidth())
                    VerticalSpace(dp = 8.dp)
                    OutlinedTextField(value = shortBreakDurationMin, onValueChange = { shortBreakDurationMin = it },
                        label = { Text(stringResource(Res.string.short_break_duration)) }, modifier = Modifier.fillMaxWidth())
                    VerticalSpace(dp = 8.dp)
                    OutlinedTextField(value = longBreakDurationMin, onValueChange = { longBreakDurationMin = it },
                        label = { Text(stringResource(Res.string.long_break_duration)) }, modifier = Modifier.fillMaxWidth())
                    VerticalSpace(dp = 8.dp)
                }
                item {
                    OutlinedTextField(value = pomodorosBeforeLongBreak, onValueChange = { pomodorosBeforeLongBreak = it },
                        label = { Text(stringResource(Res.string.pomodoros_before_long_break)) }, modifier = Modifier.fillMaxWidth())
                    VerticalSpace(dp = 16.dp)
                    PListItem(title = stringResource(Res.string.show_notification)) {
                        PSwitch(activated = showNotification) { showNotification = it }
                        HorizontalSpace(8.dp)
                    }
                    VerticalSpace(dp = 8.dp)
                    PListItem(title = stringResource(Res.string.play_sound_on_complete)) {
                        PSwitch(activated = playSoundOnComplete) { playSoundOnComplete = it }
                        HorizontalSpace(8.dp)
                    }
                    VerticalSpace(dp = 16.dp)
                }
                item {
                    PomodoroSoundSection(soundPath = soundPath, originalFileName = originalFileName,
                        onClear = { soundPath = ""; originalFileName = "" })
                }
            }
        },
        confirmButton = {
            PFilledButton(
                text = stringResource(Res.string.save),
                buttonSize = ButtonSize.MEDIUM,
                onClick = {
                    onSettingsChange(DPomodoroSettings(
                        workDurationMin = workDurationMin.toIntOrNull() ?: 25, shortBreakDurationMin = shortBreakDurationMin.toIntOrNull() ?: 5,
                        longBreakDurationMin = longBreakDurationMin.toIntOrNull() ?: 15, pomodorosBeforeLongBreak = pomodorosBeforeLongBreak.toIntOrNull() ?: 4,
                        showNotification = showNotification, playSoundOnComplete = playSoundOnComplete,
                        soundPath = soundPath, originalSoundName = originalFileName,
                    ))
                    onDismiss()
                },
            )
        },
        dismissButton = { PTextButton(text = stringResource(Res.string.cancel), onClick = onDismiss) },
    )
}
