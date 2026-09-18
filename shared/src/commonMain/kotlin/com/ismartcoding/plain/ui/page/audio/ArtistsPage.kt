package com.ismartcoding.plain.ui.page.audio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.toSortName
import com.ismartcoding.plain.ui.base.AlphabetIndexBar
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.AudioHomeArtist
import com.ismartcoding.plain.ui.models.AudioHomeViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.audio.components.ArtistAvatar
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Bucket key for names not starting with A-Z (contacts convention: last). */
private const val OTHER_BUCKET = "#"

/** Pinyin-aware bucket: 王菲 sorts under W via the project's pinyin engine. */
private fun artistBucket(sortKey: String): String {
    val first = sortKey.firstOrNull()?.uppercaseChar() ?: return OTHER_BUCKET
    return if (first in 'A'..'Z') first.toString() else OTHER_BUCKET
}

/**
 * All artists grouped A-Z with sticky letter headers and a contacts-style
 * alphabet rail. (Home keeps its most-played top-10 ordering.)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArtistsPage(
    navController: NavHostController,
    audioVM: AudioViewModel,
    homeVM: AudioHomeViewModel,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    val sections = remember(homeVM.artists.value) {
        homeVM.artists.value
            .map { it to it.name.toSortName() }
            .groupBy { (_, sortKey) -> artistBucket(sortKey) }
            .entries
            .sortedWith(compareBy({ it.key == OTHER_BUCKET }, { it.key }))
            .map { (letter, list) ->
                letter to list.sortedWith(compareBy({ it.second }, { it.first.name })).map { it.first }
            }
    }
    // Letter -> LazyColumn index of its sticky header, for the rail jumps.
    val letterIndexes = remember(sections) {
        val map = mutableMapOf<String, Int>()
        var index = 0
        sections.forEach { (letter, artists) ->
            map[letter] = index
            index += 1 + artists.size
        }
        map
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.artists),
                navController = navController,
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState) {
                sections.forEach { (letter, artists) ->
                    stickyHeader(key = "header_$letter") {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(artists.size, key = { artists[it].name }) { index ->
                        val artist = artists[index]
                        ArtistRow(artist, index) {
                            navController.navigate(Routing.ArtistDetail(artist.name))
                        }
                    }
                }
                item(key = "bottom") { VerticalSpace(24.dp) }
            }
            AlphabetIndexBar(
                letters = sections.map { it.first },
                onLetterSelect = { letter ->
                    letterIndexes[letter]?.let { target ->
                        scope.launch { scrollState.scrollToItem(target) }
                    }
                },
            )
        }
    }
}

@Composable
private fun ArtistRow(artist: AudioHomeArtist, gradientIndex: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtistAvatar(name = artist.name, gradientIndex = gradientIndex, size = 48)
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.listItemTitle(),
                maxLines = 1,
            )
            Text(
                text = pluralStringResource(Res.plurals.items, artist.itemCount, artist.itemCount),
                style = MaterialTheme.typography.listItemSubtitle(),
            )
        }
    }
}
