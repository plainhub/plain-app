package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle

@Composable
fun AudioPlayerBarCard(
    title: String,
    artist: String,
    progress: Float,
    duration: Float,
    isPlaying: Boolean,
    onClickContent: () -> Unit,
    onClickPlaylist: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(PlainTheme.CARD_RADIUS),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { if (duration == 0f) 0f else progress / duration },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).weight(1f).clickable { onClickContent() }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(text = title, style = MaterialTheme.typography.listItemTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    VerticalSpace(4.dp)
                    Text(text = artist, style = MaterialTheme.typography.listItemSubtitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { if (isPlaying) AudioPlayer.pause() else AudioPlayer.play() },
                    modifier = Modifier.size(48.dp).shadow(2.dp, CircleShape).clip(CircleShape)
                        .background(if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = painterResource(id = if (isPlaying) R.drawable.pause else R.drawable.play_arrow),
                        contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        tint = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                HorizontalSpace(8.dp)
                IconButton(onClick = onClickPlaylist, modifier = Modifier.size(42.dp).clip(CircleShape)) {
                    Icon(painter = painterResource(id = R.drawable.list_music), contentDescription = "Queue", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
