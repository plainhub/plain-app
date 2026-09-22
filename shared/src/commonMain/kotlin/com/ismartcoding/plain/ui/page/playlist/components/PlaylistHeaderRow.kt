package com.ismartcoding.plain.ui.page.playlist.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.ismartcoding.plain.ui.page.playlist.PlaylistAlbumCover
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import org.jetbrains.compose.resources.pluralStringResource

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
