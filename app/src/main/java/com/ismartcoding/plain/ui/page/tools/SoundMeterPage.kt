package com.ismartcoding.plain.ui.page.tools

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.R
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.base.*
import org.jetbrains.compose.resources.stringArrayResource
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
    val decibelValueStrings = stringArrayResource(Res.array.decibel_values)
    val decibelValueString by remember(decibel.floatValue) {
        derivedStateOf { if (decibel.floatValue > 0) decibelValueStrings.getOrNull((decibel.floatValue / 10).toInt() - 1) ?: "" else "" }
    }

    SoundMeterRecorder(audioRecord, isRunning, decibel, total, count, min, avg, max)

    PScaffold(topBar = {
        PTopAppBar(
            navController = navController,
            title = stringResource(Res.string.sound_meter), actions = {
                PIconButton(
                    icon = Res.drawable.info, contentDescription = stringResource(Res.string.decibel_values),
                    tint = MaterialTheme.colorScheme.onSurface
                ) { decibelValuesDialogVisible.value = true }
            })
    }, content = { paddingValues ->
        LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = FormatHelper.formatFloat(abs(decibel.floatValue), digits = 1),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalSpace(dp = 16.dp)
                        Text(
                            modifier = Modifier.padding(bottom = 12.dp), text = "dB",
                            style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(Res.string.min)); Text(text = FormatHelper.formatFloat(min.floatValue, digits = 1))
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(Res.string.avg)); Text(text = FormatHelper.formatFloat(avg.floatValue, digits = 1))
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(Res.string.max)); Text(text = FormatHelper.formatFloat(max.floatValue, digits = 1))
                    }
                }
                Text(
                    text = decibelValueString, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .padding(16.dp), textAlign = TextAlign.Center
                )
                if (isRunning.value) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PFilledButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.stop),
                            buttonSize = ButtonSize.EXTRA_LARGE,
                            onClick = { isRunning.value = false })
                        PFilledButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.reset),
                            buttonSize = ButtonSize.EXTRA_LARGE,
                            type = ButtonType.DANGER,
                            onClick = {
                                total.floatValue = 0f; count.intValue = 0; decibel.floatValue = 0f
                                min.floatValue = 0f; max.floatValue = 0f; avg.floatValue = 0f
                            })
                    }
                } else if (count.intValue > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PFilledButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.start),
                            buttonSize = ButtonSize.EXTRA_LARGE,
                            onClick = {
                                if (Permission.RECORD_AUDIO.can(context)) isRunning.value = true
                                else sendEvent(RequestPermissionsEvent(Permission.RECORD_AUDIO))
                            })
                        PFilledButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.reset),
                            buttonSize = ButtonSize.EXTRA_LARGE,
                            type = ButtonType.DANGER,
                            onClick = {
                                total.floatValue = 0f; count.intValue = 0; decibel.floatValue = 0f
                                min.floatValue = 0f; max.floatValue = 0f; avg.floatValue = 0f
                            })
                    }
                } else {
                    PFilledButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        text = stringResource(Res.string.start),
                        buttonSize = ButtonSize.EXTRA_LARGE,
                        onClick = {
                            if (Permission.RECORD_AUDIO.can(context)) isRunning.value = true
                            else sendEvent(RequestPermissionsEvent(Permission.RECORD_AUDIO))
                        })
                }
                BottomSpace(paddingValues)
            }
        }
    })

    if (decibelValuesDialogVisible.value) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { decibelValuesDialogVisible.value = false },
            confirmButton = { PFilledButton(text = stringResource(Res.string.close), buttonSize = ButtonSize.MEDIUM, onClick = { decibelValuesDialogVisible.value = false }) },
            title = { Text(text = stringResource(Res.string.decibel_values), style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    decibelValueStrings.forEach {
                        SelectionContainer { Text(text = it, style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface), modifier = Modifier.padding(bottom = 8.dp)) }
                    }
                }
            })
    }
}
