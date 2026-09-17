package com.ismartcoding.plain.ui.page.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatDuration
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.CheckCircle
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.PlaylistAddSongsViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.page.audio.components.PlaylistCoverArtwork
import com.ismartcoding.plain.ui.page.audio.components.audioHomeGradientIndexOf
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Multi-select library picker for a playlist. Tracks already in it start
 * checked; unchecking removes them on confirm. Uses the standard
 * [ListSearchBar] search flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistAddSongsPage(
    navController: NavHostController,
    playlistId: String,
) {
    val scope = rememberCoroutineScope()
    val vm: PlaylistAddSongsViewModel = viewModel { PlaylistAddSongsViewModel() }
    val scrollState = rememberLazyListState()

    LaunchedEffect(playlistId) {
        vm.loadAsync(playlistId)
    }

    val toAdd = vm.selected.value - vm.existing.value
    val toRemove = vm.existing.value - vm.selected.value
    val changed = vm.selected.value != vm.existing.value
    // Computed here (composable scope) so the toast plural resolves.
    val addedMsg = pluralStringResource(Res.plurals.added_n_items, toAdd.size, toAdd.size)

    PScaffold(
        topBar = {
            if (vm.showSearchBar.value) {
                ListSearchBar(viewModel = vm, onSearch = { scope.launch { vm.searchAsync() } })
            } else {
                PTopAppBar(
                    title = stringResource(Res.string.add_items),
                    navController = navController,
                    actions = {
                        PIconButton(
                            icon = Res.drawable.search,
                            contentDescription = stringResource(Res.string.search),
                            click = { vm.enterSearchMode() },
                        )
                    },
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Text(
                text = pluralStringResource(Res.plurals.items, vm.items.value.size, vm.items.value.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f), state = scrollState) {
                items(vm.items.value.size, key = { vm.items.value[it].path }) { index ->
                    val song = vm.items.value[index]
                    val isSelected = song.path in vm.selected.value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                vm.selected.value =
                                    if (isSelected) vm.selected.value - song.path else vm.selected.value + song.path
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaylistCoverArtwork(
                            gradientIndex = audioHomeGradientIndexOf(song.path),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            iconSize = 20,
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp, end = 8.dp),
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.listItemTitle(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${song.artist} · ${song.duration.formatDuration()}",
                                style = MaterialTheme.typography.listItemSubtitle(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        CheckCircle(
                            selected = isSelected,
                            modifier = Modifier.padding(end = 8.dp),
                            onClick = {
                                vm.selected.value =
                                    if (isSelected) vm.selected.value - song.path else vm.selected.value + song.path
                            },
                        )
                    }
                }
                item { VerticalSpace(24.dp) }
            }
            PFilledButton(
                text = LocaleHelper.getStringF(Res.string.add_n_items, vm.selected.value.size),
                enabled = changed,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                onClick = {
                    scope.launch {
                        withIO {
                            val audioByPath = vm.items.value.associateBy { it.path }
                            val items = toAdd.mapNotNull { path -> audioByPath[path]?.toPlaylistAudio() }
                            AudioQueueManager.addPlaylistSongs(playlistId, items)
                            toRemove.forEach { AudioQueueManager.removePlaylistSong(playlistId, it) }
                        }
                        DialogHelper.showMessage(addedMsg)
                        navController.popBackStack()
                    }
                },
            )
        }
    }
}
