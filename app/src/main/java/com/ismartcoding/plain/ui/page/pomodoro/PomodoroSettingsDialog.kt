package com.ismartcoding.plain.ui.page.pomodoro

import androidx.compose.foundation.layout.Arrangement
import com.ismartcoding.lib.logcat.LogCat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.queryOpenableFile
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.launch
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsDialog(
    settings: DPomodoroSettings,
    onSettingsChange: (DPomodoroSettings) -> Unit,
    onDismiss: () -> Unit
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
    var originalFileName by remember {
        mutableStateOf(settings.originalSoundName)
    }

    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.POMODORO) {
                        return@collect
                    }
                    if (event.uris.isNotEmpty()) {
                        val uri = event.uris.first()
                        scope.launch {
                            try {
                                val file = context.contentResolver.queryOpenableFile(uri)
                                if (file != null) {
                                    originalFileName = file.displayName
                                    val audioDir = File(context.getExternalFilesDir(null), "audio")
                                    if (!audioDir.exists()) {
                                        audioDir.mkdirs()
                                    }
                                    val fileName = "pomodoro_sound.mp3"
                                    val dstFile = File(audioDir, fileName)

                                    FileHelper.copyFile(context, uri, dstFile.absolutePath)
                                    soundPath = "app://audio/$fileName"
                                }
                            } catch (e: Exception) {
                                LogCat.e("Failed to copy pomodoro sound file: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn {
                item {
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
                }
                item {
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

                    VerticalSpace(dp = 16.dp)
                }
                item {

                    Column {
                        Text(
                            text = stringResource(R.string.custom_sound),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            if (soundPath.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = originalFileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                sendEvent(PickFileEvent(PickFileTag.POMODORO, PickFileType.FILE, multiple = false))
                                            }
                                        ) {
                                            Text(stringResource(R.string.change))
                                        }

                                        TextButton(
                                            onClick = {
                                                soundPath = ""
                                                originalFileName = ""
                                            }
                                        ) {
                                            Text(
                                                text = stringResource(R.string.clear),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    TextButton(
                                        onClick = {
                                            sendEvent(PickFileEvent(PickFileTag.POMODORO, PickFileType.FILE, multiple = false))
                                        }
                                    ) {
                                        Text(stringResource(R.string.select_sound))
                                    }
                                }
                            }
                        }
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
                        playSoundOnComplete = playSoundOnComplete,
                        soundPath = soundPath,
                        originalSoundName = originalFileName
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