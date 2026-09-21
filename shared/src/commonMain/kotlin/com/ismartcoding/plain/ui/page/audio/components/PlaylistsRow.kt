package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DAudioPlaylist
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.drawerOpenAtRowStart
import com.ismartcoding.plain.ui.page.playlist.PlaylistAlbumCover
import com.ismartcoding.plain.ui.page.playlist.components.PlaylistMosaicCover
import com.ismartcoding.plain.ui.theme.listItemTitle
import org.jetbrains.compose.resources.pluralStringResource

/** Horizontally scrolling playlist cards. */
@Composable
fun PlaylistsRow(
    playlists: List<Pair<DAudioPlaylist, Int>>,
    covers: Map<String, List<PlaylistAlbumCover>>,
    onPlaylistClick: (DAudioPlaylist) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().drawerOpenAtRowStart(listState),
    ) {
        items(playlists.size, key = { playlists[it].first.id }) { index ->
            val (pl, count) = playlists[index]
            Column(
                modifier = Modifier
                    .width(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlaylistClick(pl) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlaylistMosaicCover(
                    albums = covers[pl.id].orEmpty(),
                    gradientIndex = index + 2,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Text(
                    text = pl.name,
                    style = MaterialTheme.typography.listItemTitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = pluralStringResource(Res.plurals.items, count, count),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
