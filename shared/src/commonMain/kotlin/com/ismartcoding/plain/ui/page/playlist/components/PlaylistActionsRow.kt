package com.ismartcoding.plain.ui.page.playlist.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Play-all / shuffle action pair; disabled for an empty playlist. */
@Composable
fun PlaylistActionsRow(
    enabled: Boolean,
    isPlaying: Boolean = false,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        PFilledButton(
            text = stringResource(Res.string.play_all),
            // Standard player pattern: the play button becomes pause while
            // this playlist is the active playing source.
            icon = painterResource(if (isPlaying) Res.drawable.pause else Res.drawable.play_arrow),
            modifier = Modifier.weight(1f),
            enabled = enabled,
            onClick = onPlayAll,
        )
        Spacer(Modifier.width(12.dp))
        POutlinedButton(
            text = stringResource(Res.string.shuffle_play),
            icon = painterResource(Res.drawable.shuffle),
            modifier = Modifier.weight(1f),
            enabled = enabled,
            onClick = onShuffle,
        )
    }
}
