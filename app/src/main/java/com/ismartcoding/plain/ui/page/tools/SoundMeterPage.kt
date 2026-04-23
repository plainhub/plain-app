package com.ismartcoding.plain.ui.page.tools

import android.media.AudioRecord
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.base.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundMeterPage(navController: NavHostController) {
    val context = LocalContext.current
    var decibelValuesDialogVisible = remember { mutableStateOf(false) }
    val audioRecord = remember { mutableStateOf<AudioRecord?>(null) }
    val total = remember { mutableFloatStateOf(0f) }
    val count = remember { mutableIntStateOf(0) }
    val min = remember { mutableFloatStateOf(0f) }
    val avg = remember { mutableFloatStateOf(0f) }
    val max = remember { mutableFloatStateOf(0f) }
    val isRunning = remember { mutableStateOf(false) }
    val decibel = remember { mutableFloatStateOf(0f) }
    val decibelValueStrings = stringArrayResource(R.array.decibel_values)
    val decibelValueString by remember(decibel.floatValue) {
        derivedStateOf { if (decibel.floatValue > 0) decibelValueStrings.getOrNull((decibel.floatValue / 10).toInt() - 1) ?: "" else "" }
    }

    SoundMeterRecorder(audioRecord, isRunning, decibel, total, count, min, avg, max)

    PScaffold(topBar = {
        PTopAppBar(navController = navController,
            title = stringResource(R.string.sound_meter), actions = {
                PIconButton(icon = R.drawable.info, contentDescription = stringResource(R.string.decibel_values),
                    tint = MaterialTheme.colorScheme.onSurface) { decibelValuesDialogVisible.value = true }
            })
    }, content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = FormatHelper.formatFloat(abs(decibel.floatValue), digits = 1),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalSpace(dp = 16.dp)
                        Text(modifier = Modifier.padding(bottom = 12.dp), text = "dB",
                            style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.min)); Text(text = FormatHelper.formatFloat(min.floatValue, digits = 1))
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.avg)); Text(text = FormatHelper.formatFloat(avg.floatValue, digits = 1))
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.max)); Text(text = FormatHelper.formatFloat(max.floatValue, digits = 1))
                    }
                }
                Text(text = decibelValueString, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().height(96.dp).padding(16.dp), textAlign = TextAlign.Center)
                if (isRunning.value) {
                    PFilledButton(text = stringResource(R.string.stop), modifier = Modifier.padding(horizontal = 16.dp), onClick = { isRunning.value = false })
                } else {
                    PFilledButton(text = stringResource(R.string.start), modifier = Modifier.padding(horizontal = 16.dp), onClick = {
                        if (Permission.RECORD_AUDIO.can(context)) isRunning.value = true
                        else sendEvent(RequestPermissionsEvent(Permission.RECORD_AUDIO))
                    })
                }
                if (count.intValue > 0) {
                    VerticalSpace(dp = 40.dp)
                    PFilledButton(text = stringResource(R.string.reset), type = ButtonType.DANGER, modifier = Modifier.padding(horizontal = 16.dp), onClick = {
                        total.floatValue = 0f; count.intValue = 0; decibel.floatValue = 0f
                        min.floatValue = 0f; max.floatValue = 0f; avg.floatValue = 0f
                    })
                }
                BottomSpace(paddingValues)
            }
        }
    })

    if (decibelValuesDialogVisible.value) {
        AlertDialog(containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { decibelValuesDialogVisible.value = false },
            confirmButton = { Button(onClick = { decibelValuesDialogVisible.value = false }) { Text(stringResource(R.string.close)) } },
            title = { Text(text = stringResource(R.string.decibel_values), style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    decibelValueStrings.forEach {
                        SelectionContainer { Text(text = it, style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface), modifier = Modifier.padding(bottom = 8.dp)) }
                    }
                }
            })
    }
}
