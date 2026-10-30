package com.ismartcoding.plain.ui.navigation

import com.ismartcoding.plain.rust.graphql.ContentSource
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.ismartcoding.plain.ui.audio.*
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.audio.playlists.PlaylistDetailPage
import com.ismartcoding.plain.ui.audio.playlists.PlaylistAddItemsPage
import com.ismartcoding.plain.ui.files.MediaFoldersViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel

private data class AudioNavigationState(
    val queue: AudioQueueViewModel,
    val library: AudioViewModel,
    val tags: TagsViewModel,
    val folders: MediaFoldersViewModel,
    val cast: CastViewModel,
    val home: AudioHomeViewModel,
)

@Composable
private fun audioNavigationState(owner: NavBackStackEntry): AudioNavigationState {
    val source = LocalContentSource.current as ContentSource.Remote
    val queue = viewModel(viewModelStoreOwner = owner, key = "audio-queue") { AudioQueueViewModel(source) }
    val library = viewModel(viewModelStoreOwner = owner, key = "audio-library") { AudioViewModel(source) }
    val tags = viewModel(viewModelStoreOwner = owner, key = "audio-tags") { TagsViewModel(source.tags) }
    val folders = viewModel(viewModelStoreOwner = owner, key = "audio-folders") { MediaFoldersViewModel(source) }
    val cast = viewModel(viewModelStoreOwner = owner, key = "audio-cast") { CastViewModel(source) }
    val home = viewModel(viewModelStoreOwner = owner, key = "audio-home") { AudioHomeViewModel(source) }
    return AudioNavigationState(queue, library, tags, folders, cast, home)
}

internal fun NavGraphBuilder.addManagedAudioRoutes(
    navController: NavHostController,
    owner: NavBackStackEntry,
    manager: com.ismartcoding.plain.ui.tools.ManagedDeviceViewModel,
) {
    val source: @Composable () -> ContentSource? = { managedScope(manager) }
    contentDestination<Routing.Audio>(source) {
        val state = audioNavigationState(owner)
        AudioHomePage(navController, state.queue, state.library, state.tags, state.folders, state.cast, state.home)
    }
    contentDestination<Routing.AudioAll>(source) {
        val state = audioNavigationState(owner)
        AudioAllPage(navController, state.queue, state.library, state.tags, state.folders, state.cast)
    }
    contentDestination<Routing.Artists>(source) {
        val state = audioNavigationState(owner)
        ArtistsPage(navController, state.library, state.home)
    }
    contentDestination<Routing.ArtistDetail>(source) { entry ->
        val state = audioNavigationState(owner)
        AudioArtistPage(navController, entry.toRoute<Routing.ArtistDetail>().name, state.queue, state.library, state.tags, state.folders, state.cast)
    }
    contentDestination<Routing.PlaylistDetail>(source) { entry ->
        val state = audioNavigationState(owner)
        PlaylistDetailPage(navController, entry.toRoute<Routing.PlaylistDetail>().id, state.queue, state.library, state.tags, state.folders, state.cast)
    }
    contentDestination<Routing.PlaylistAddItems>(source) { entry ->
        PlaylistAddItemsPage(navController, entry.toRoute<Routing.PlaylistAddItems>().id)
    }
}
