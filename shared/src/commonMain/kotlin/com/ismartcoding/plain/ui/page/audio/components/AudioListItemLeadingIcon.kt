package com.ismartcoding.plain.ui.page.audio.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import com.ismartcoding.plain.ui.components.CheckCircle
import com.ismartcoding.plain.ui.components.PulsatingWave
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.cast as ui_drawable_cast
import com.ismartcoding.plain.i18n.cast

@Composable
fun AudioListItemLeadingIcon(
    item: DAudio,
    dragSelectState: DragSelectState,
    castMode: Boolean,
    isCurrentItemLoading: Boolean,
    isCurrentlyPlayingByCast: Boolean,
    isCurrentlyPlaying: Boolean,
    coverContent: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        if (dragSelectState.selectMode) {
            CheckCircle(selected = dragSelectState.isSelected(item.id), onClick = { dragSelectState.select(item.id) })
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
                        painter = painterResource(UiRes.drawable.ui_drawable_cast),
                        contentDescription = stringResource(Res.string.cast),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else if (!isCurrentlyPlaying) {
            coverContent()
        } else {
            PulsatingWave(isPlaying = true, modifier = Modifier.align(Alignment.Center))
        }
    }
}
