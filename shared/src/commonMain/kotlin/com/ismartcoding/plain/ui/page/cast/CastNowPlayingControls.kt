package com.ismartcoding.plain.ui.page.cast

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.pause as ui_drawable_pause
import com.ismartcoding.plain.ui.resources.play_arrow as ui_drawable_play_arrow
import com.ismartcoding.plain.i18n.pause

/** Now-playing controls shown at the top of the cast playlist sheet. */
@Composable
internal fun CastNowPlayingControls(
    title: String,
    artist: String,
    isPlaying: Boolean,
    progressMs: Float,
    durationMs: Float,
    supportsCallback: Boolean,
    deviceName: String,
    onPlay: () -> Unit,
    onPause: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = {
                if (supportsCallback && durationMs > 0f) progressMs / durationMs else 0f
            },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (title.isNotEmpty()) title else stringResource(Res.string.casting),
                    style = MaterialTheme.typography.listItemTitle(),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                VerticalSpace(4.dp)
                Text(
                    text = if (artist.isNotEmpty()) artist else deviceName,
                    style = MaterialTheme.typography.listItemSubtitle(),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { if (isPlaying) onPause() else onPlay() },
                modifier = Modifier.size(48.dp).shadow(2.dp, CircleShape).clip(CircleShape)
                    .background(if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) UiRes.drawable.ui_drawable_pause else UiRes.drawable.ui_drawable_play_arrow),
                    contentDescription = if (isPlaying) stringResource(Res.string.pause) else stringResource(Res.string.play),
                    tint = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            HorizontalSpace(8.dp)
        }
    }
}
