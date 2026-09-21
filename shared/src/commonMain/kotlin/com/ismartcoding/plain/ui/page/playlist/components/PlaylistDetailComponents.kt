package com.ismartcoding.plain.ui.page.playlist.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.ui.page.playlist.PlaylistAlbumCover
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Hero row: cover mosaic + name + "N items" summary. */
@Composable
fun PlaylistHeaderRow(
    name: String,
    itemCount: Int,
    albums: List<PlaylistAlbumCover>,
    gradientIndex: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistMosaicCover(
            albums = albums,
            gradientIndex = gradientIndex,
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)),
            iconSize = 36,
        )
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.listItemTitle(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(Res.plurals.items, itemCount, itemCount),
                style = MaterialTheme.typography.listItemSubtitle(),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

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
