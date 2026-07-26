package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Speed picker button shared by the video and audio players.
 *
 * Shows the gauge icon at 1x (default) and the speed label (e.g. "3x") otherwise,
 * so the user can tell at a glance when playback is not at normal speed.
 *
 * @param speed current playback speed
 * @param onSpeedChange called with the newly selected speed
 * @param tint color for the icon / label
 * @param modifier applied to the [IconButton] (e.g. background / shape)
 */
@Composable
fun PlaybackSpeedButton(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDefault = speed == 1f
    val currentLabel = remember(speed) { PLAYBACK_SPEEDS.firstOrNull { it.speed == speed }?.label ?: "1x" }
    fun apply(s: Float) { showMenu = false; onSpeedChange(s) }

    Box {
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            PLAYBACK_SPEEDS.forEach { item ->
                DropdownMenuItem(
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = { apply(item.speed) },
                    leadingIcon = { RadioButton(selected = speed == item.speed, onClick = { apply(item.speed) }) },
                    text = { Text(text = item.label) },
                )
            }
        }
        IconButton(onClick = { showMenu = !showMenu }, modifier = modifier) {
            if (isDefault) {
                Icon(
                    painter = painterResource(Res.drawable.gauge),
                    contentDescription = stringResource(Res.string.change_playback_speed),
                    tint = tint,
                )
            } else {
                Text(
                    text = currentLabel,
                    color = tint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
