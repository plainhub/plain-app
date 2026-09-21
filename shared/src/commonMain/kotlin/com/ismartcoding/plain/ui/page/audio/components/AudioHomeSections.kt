package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.drawerOpenAtRowStart
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.ui.models.AudioHomeArtist
import com.ismartcoding.plain.ui.page.playlist.PlaylistAlbumCover
import com.ismartcoding.plain.ui.page.playlist.components.PlaylistMosaicCover
import com.ismartcoding.plain.ui.theme.listItemTitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Section title row with an optional trailing "View all" action. */
@Composable
fun HomeSectionHeader(title: String, onViewAll: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.listItemTitle(),
            modifier = Modifier.weight(1f),
        )
        if (onViewAll != null) {
            Text(
                text = stringResource(Res.string.view_all),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewAll)
                    .padding(8.dp),
            )
        }
    }
}

/** Quick actions: shuffle the whole library / browse all items. */
@Composable
fun HomeQuickActions(
    onShuffleAll: () -> Unit,
    onViewAllItems: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        PFilledButton(
            text = stringResource(Res.string.shuffle_play),
            icon = painterResource(Res.drawable.shuffle),
            modifier = Modifier.weight(1f),
            onClick = onShuffleAll,
        )
        Spacer(Modifier.width(12.dp))
        POutlinedButton(
            text = stringResource(Res.string.view_all),
            icon = painterResource(Res.drawable.music2),
            modifier = Modifier.weight(1f),
            onClick = onViewAllItems,
        )
    }
}

/** Horizontally scrolling artist tiles. */
@Composable
fun ArtistsRow(
    artists: List<AudioHomeArtist>,
    onArtistClick: (AudioHomeArtist) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().drawerOpenAtRowStart(listState),
    ) {
        items(artists.size, key = { artists[it].name }) { index ->
            val artist = artists[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onArtistClick(artist) }
                    .padding(4.dp),
            ) {
                ArtistAvatar(name = artist.name, artworkPath = artist.samplePath, size = 64.dp)
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Horizontally scrolling playlist cards with the trailing "new playlist" tile. */
@Composable
fun PlaylistsRow(
    playlists: List<Pair<DAudioPlaylist, Int>>,
    covers: Map<String, List<PlaylistAlbumCover>>,
    onNewPlaylist: () -> Unit,
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
        item(key = "new") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(4.dp)
                    .size(132.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable(onClick = onNewPlaylist),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.plus),
                    contentDescription = stringResource(Res.string.new_playlist),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = stringResource(Res.string.new_playlist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
