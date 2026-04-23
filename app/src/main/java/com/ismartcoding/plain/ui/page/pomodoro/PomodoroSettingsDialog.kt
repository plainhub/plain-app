package com.ismartcoding.plain.ui.page.pomodoro

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.appDir
import com.ismartcoding.lib.extensions.queryOpenableFile
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsDialog(
    settings: DPomodoroSettings, onSettingsChange: (DPomodoroSettings) -> Unit, onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var workDuration by remember { mutableStateOf(settings.workDuration.toString()) }
    var shortBreakDuration by remember { mutableStateOf(settings.shortBreakDuration.toString()) }
    var longBreakDuration by remember { mutableStateOf(settings.longBreakDuration.toString()) }
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
                        val file = context.contentResolver.queryOpenableFile(event.uris.first())
                        if (file != null) {
                            originalFileName = file.displayName
                            val audioDir = File(context.appDir(), "audio")
                            if (!audioDir.exists()) audioDir.mkdirs()
                            val dstFile = File(audioDir, "pomodoro_sound.mp3")
                            FileHelper.copyFile(context, event.uris.first(), dstFile.absolutePath)
                            soundPath = "app://audio/pomodoro_sound.mp3"
                        }
                    } catch (e: Exception) { LogCat.e("Failed to copy pomodoro sound file: ${e.message}") }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(value = workDuration, onValueChange = { workDuration = it },
                        label = { Text(stringResource(R.string.work_duration)) }, modifier = Modifier.fillMaxWidth())
                    VerticalSpace(dp = 8.dp)
                    OutlinedTextField(value = shortBreakDuration, onValueChange = { shortBreakDuration = it },
                        label = { Text(stringResource(R.string.short_break_duration)) }, modifier = Modifier.fillMaxWidth())
                    VerticalSpace(dp = 8.dp)
                    OutlinedTextField(value = longBreakDuration, onValueChange = { longBreakDuration = it },
                        label = { Text(stringResource(R.string.long_break_duration)) }, modifier = Modifier.fillMaxWidth())
                    VerticalSpace(dp = 8.dp)
                }
                item {
                    OutlinedTextField(value = pomodorosBeforeLongBreak, onValueChange = { pomodorosBeforeLongBreak = it },
                        label = { Text(stringResource(R.string.pomodoros_before_long_break)) }, modifier = Modifier.fillMaxWidth())
                    VerticalSpace(dp = 16.dp)
                    PDialogListItem(title = stringResource(R.string.show_notification)) { PSwitch(activated = showNotification) { showNotification = it } }
                    VerticalSpace(dp = 8.dp)
                    PDialogListItem(title = stringResource(R.string.play_sound_on_complete)) { PSwitch(activated = playSoundOnComplete) { playSoundOnComplete = it } }
                    VerticalSpace(dp = 16.dp)
                }
                item {
                    PomodoroSoundSection(soundPath = soundPath, originalFileName = originalFileName,
                        onClear = { soundPath = ""; originalFileName = "" })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSettingsChange(DPomodoroSettings(
                    workDuration = workDuration.toIntOrNull() ?: 25, shortBreakDuration = shortBreakDuration.toIntOrNull() ?: 5,
                    longBreakDuration = longBreakDuration.toIntOrNull() ?: 15, pomodorosBeforeLongBreak = pomodorosBeforeLongBreak.toIntOrNull() ?: 4,
                    showNotification = showNotification, playSoundOnComplete = playSoundOnComplete,
                    soundPath = soundPath, originalSoundName = originalFileName,
                ))
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
