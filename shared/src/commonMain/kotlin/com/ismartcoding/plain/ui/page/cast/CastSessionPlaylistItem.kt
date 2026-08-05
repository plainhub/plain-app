package com.ismartcoding.plain.ui.page.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.data.IMedia
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.file
import com.ismartcoding.plain.i18n.playlist_remove
import com.ismartcoding.plain.i18n.remove_from_cast_queue
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.platform.getAudioMetadata
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.red
import com.ismartcoding.plain.ui.theme.secondaryTextColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.text.ifEmpty

@Composable
fun CastSessionPlaylistItem(
    item: IMedia,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }

    LaunchedEffect(item.path) {
        if (item is DAudio) {
            val (t, a) = withIO { getAudioMetadata(item.path) }
            title = t; subtitle = a
        } else {
            title = item.title.ifEmpty { item.path.getFilenameWithoutExtensionFromPath() }
            subtitle = ""
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.cardBackgroundNormal)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                item.path.isImageFast() -> AsyncImage(
                    model = item.path,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                item.path.isVideoFast() -> AsyncImage(
                    model = item.path,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                item.path.isAudioFast() -> AudioCoverArt(path = item.path, modifier = Modifier.size(40.dp))
                else -> Icon(
                    painter = painterResource(Res.drawable.file),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondaryTextColor,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.listItemTitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.listItemSubtitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        PIconButton(
            icon = Res.drawable.playlist_remove,
            tint = MaterialTheme.colorScheme.red,
            contentDescription = stringResource(Res.string.remove_from_cast_queue),
            click = onRemove,
        )
    }
}
