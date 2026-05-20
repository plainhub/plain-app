package com.ismartcoding.plain.ui.page.audio.components

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.PulsatingWave
import com.ismartcoding.plain.ui.theme.blue

@Composable
fun AudioListItemLeadingIcon(
    item: DAudio,
    dragSelectState: DragSelectState,
    castMode: Boolean,
    isCurrentItemLoading: Boolean,
    isCurrentlyPlayingByCast: Boolean,
    isCurrentlyPlaying: Boolean,
) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        if (dragSelectState.selectMode) {
            Checkbox(
                checked = dragSelectState.isSelected(item.id),
                onCheckedChange = { dragSelectState.select(item.id) }
            )
        } else if (castMode) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentItemLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else if (isCurrentlyPlayingByCast) {
                    PulsatingWave(isPlaying = true, modifier = Modifier.align(Alignment.Center))
                } else {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(Res.drawable.cast),
                        contentDescription = stringResource(Res.string.cast),
                        tint = MaterialTheme.colorScheme.blue
                    )
                }
            }
        } else if (!isCurrentlyPlaying) {
            AudioCoverOrIcon(path = item.path, modifier = Modifier.size(40.dp))
        } else {
            PulsatingWave(isPlaying = true, modifier = Modifier.align(Alignment.Center))
        }
    }
}
