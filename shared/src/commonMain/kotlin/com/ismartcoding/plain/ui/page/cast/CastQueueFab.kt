package com.ismartcoding.plain.ui.page.cast

import com.ismartcoding.plain.i18n.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.cast as ui_drawable_cast
import com.ismartcoding.plain.i18n.cast

/**
 * Cast-mode entry to the cast playlist: replaces the old bottom cast player
 * bar with a corner FAB so no bottom bar can be mistaken for on-device
 * playback. The sheet itself carries the now-playing controls plus the queue.
 */
@Composable
fun CastQueueFab(
    castVM: CastViewModel,
    modifier: Modifier = Modifier,
    dragSelectState: DragSelectState,
) {
    var showCastPlaylist by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = castVM.castMode.value && castVM.hasCurrentDevice && !dragSelectState.selectMode,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut() + scaleOut(targetScale = 0.6f),
        modifier = modifier,
    ) {
        FloatingActionButton(
            onClick = { showCastPlaylist = true },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.navigationBarsPadding().padding(16.dp),
        ) {
            Icon(
                painter = painterResource(UiRes.drawable.ui_drawable_cast),
                contentDescription = stringResource(Res.string.cast_playlist),
            )
        }
    }

    if (showCastPlaylist) {
        AudioCastPlaylistPage(castVM = castVM, onDismissRequest = { showCastPlaylist = false })
    }
}
