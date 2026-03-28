package com.ismartcoding.plain.ui.page.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle

@Composable
internal fun AudioCastPlayerBarContent(
    castVM: CastViewModel,
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    duration: Float,
    supportsCallback: Boolean,
    currentUri: String,
    onShowPlaylist: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = {
                if (supportsCallback && duration > 0f) progress / duration else 0f
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
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).weight(1f)
                    .clickable { onShowPlaylist() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (title.isNotEmpty()) title else stringResource(R.string.casting),
                    style = MaterialTheme.typography.listItemTitle(),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                VerticalSpace(4.dp)
                Text(
                    text = if (artist.isNotEmpty()) artist else (CastPlayer.currentDevice?.description?.device?.friendlyName ?: ""),
                    style = MaterialTheme.typography.listItemSubtitle(),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (currentUri.isNotEmpty()) {
                IconButton(
                    onClick = { if (isPlaying) castVM.pauseCast() else castVM.playCast() },
                    modifier = Modifier.size(48.dp).shadow(2.dp, CircleShape).clip(CircleShape)
                        .background(if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        painter = painterResource(id = if (isPlaying) R.drawable.pause else R.drawable.play_arrow),
                        contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        tint = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                HorizontalSpace(8.dp)
            }
            IconButton(
                onClick = { onShowPlaylist() },
                modifier = Modifier.size(42.dp).clip(CircleShape),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.list_music),
                    contentDescription = stringResource(R.string.playlist),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
