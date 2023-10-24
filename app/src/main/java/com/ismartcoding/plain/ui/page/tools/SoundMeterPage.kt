package com.ismartcoding.plain.ui.page.tools

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionResultEvent
import com.ismartcoding.plain.features.RequestPermissionEvent
import com.ismartcoding.plain.helpers.SoundMeterHelper
import com.ismartcoding.plain.ui.base.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundMeterPage(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var decibelValuesDialogVisible by remember { mutableStateOf(false) }
    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    var total by remember { mutableFloatStateOf(0f) }
    var count by remember { mutableIntStateOf(0) }
    var min by remember { mutableFloatStateOf(0f) }
    var avg by remember { mutableFloatStateOf(0f) }
    var max by remember { mutableFloatStateOf(0f) }
    var isRunning by remember { mutableStateOf(false) }
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }
    var decibel by remember { mutableFloatStateOf(0f) }
    val decibelValueStrings = stringArrayResource(R.array.decibel_values)
    val decibelValueString by remember {
        derivedStateOf {
            if (decibel > 0) {
                return@derivedStateOf decibelValueStrings.getOrNull((decibel / 10).toInt() - 1) ?: ""
            }

            ""
        }
    }

    LaunchedEffect(Unit) {
        events.add(
            receiveEventHandler<PermissionResultEvent> {
                isRunning = Permission.RECORD_AUDIO.can(context)
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            events.forEach { it.cancel() }
        }
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) {
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
            }
            return@LaunchedEffect
        }

        val bufferSize =
            AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        val buffer = ShortArray(bufferSize)
        audioRecord =
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
            audioRecord?.startRecording()
        }
        scope.launch(Dispatchers.IO) {
            while (isRunning) {
                if (audioRecord != null) {
                    val readSize = audioRecord!!.read(buffer, 0, bufferSize)
                    if (readSize > 0) {
                        val amplitudeValue = SoundMeterHelper.getMaxAmplitude(buffer, readSize)
                        val value = abs(SoundMeterHelper.amplitudeToDecibel(amplitudeValue))
                        if (value.isFinite()) {
                            decibel = value
                            total += value
                            count++
                            avg = total / count
                            if (value > max) {
                                max = value
                            }
                            if (value < min || min == 0f) {
                                min = value
                            }
                        }
                    }
                }
                delay(180)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.stop()
                audioRecord?.release()
            }
        }
    }

    PScaffold(
        navController,
        topBarTitle = stringResource(id = R.string.sound_meter),
        actions = {
            PIconButton(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.decibel_values),
                tint = MaterialTheme.colorScheme.onSurface,
            ) {
                decibelValuesDialogVisible = true
            }
        },
        content = {
            LazyColumn {
                item {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = FormatHelper.formatFloat(abs(decibel), digits = 1),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            HorizontalSpace(dp = 16.dp)
                            Text(
                                modifier = Modifier.padding(bottom = 12.dp),
                                text = "dB",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(text = stringResource(id = R.string.min))
                            Text(text = FormatHelper.formatFloat(min, digits = 1))
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(text = stringResource(id = R.string.avg))
                            Text(text = FormatHelper.formatFloat(avg, digits = 1))
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(text = stringResource(id = R.string.max))
                            Text(text = FormatHelper.formatFloat(max, digits = 1))
                        }
                    }
                    Text(
                        text = decibelValueString,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .padding(16.dp),
                        textAlign = TextAlign.Center,
                    )
                    BlockOutlineButton(text = stringResource(id = if (isRunning) R.string.stop else R.string.start)) {
                        if (isRunning) {
                            isRunning = false
                        } else {
                            if (Permission.RECORD_AUDIO.can(context)) {
                                isRunning = true
                            } else {
                                sendEvent(RequestPermissionEvent(Permission.RECORD_AUDIO))
                            }
                        }
                    }
                    if (count > 0) {
                        VerticalSpace(dp = 40.dp)
                        BlockOutlineButton(text = stringResource(id = R.string.reset)) {
                            total = 0f
                            count = 0
                            decibel = 0f
                            min = 0f
                            max = 0f
                            avg = 0f
                        }
                    }
                    BottomSpace()
                }
            }
        },
    )

    if (decibelValuesDialogVisible) {
        PDialog(onClose = {
            decibelValuesDialogVisible = false
        }) {
            LazyColumn {
                item {
                    DisplayText(title = stringResource(id = R.string.decibel_values))
                    decibelValueStrings.forEach {
                        SelectionContainer {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                            )
                        }
                    }
                    BottomSpace()
                }
            }
        }
    }
}
