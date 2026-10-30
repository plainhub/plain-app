package com.ismartcoding.plain.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.ismartcoding.plain.features.tags.TagsApi
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel

internal fun NavGraphBuilder.addDeviceContentRoutes(
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel,
    localPeers: com.ismartcoding.plain.ui.chat.PeerViewModel,
    localChannels: com.ismartcoding.plain.ui.chat.ChannelViewModel,
    localNoteTags: TagsViewModel,
    source: @Composable () -> ContentSource?,
) {
    addMainFilePages(navController, audioQueueVM, source)
    addNotesPages(navController) {
        source()?.let { source ->
            val owner = navController.getBackStackEntry<Routing.Home>()
            if (source is ContentSource.Local) return@addNotesPages NotesNavigationState(NotesViewModel.local, localNoteTags)
            source as ContentSource.Remote
            val tagsApi = remember(source.client) { TagsApi(source.api) }
            val notes = viewModel(viewModelStoreOwner = owner, key = "notes") { NotesViewModel(source.client.notes, tagsApi) }
            val tags = viewModel(viewModelStoreOwner = owner, key = "note-tags") { TagsViewModel(tagsApi) }
            NotesNavigationState(notes, tags, source)
        }
    }
    contentDestination<Routing.ChatInfo>(source) {
        if (LocalContentSource.current is ContentSource.Local) {
            val chat = com.ismartcoding.plain.ui.chat.ChatViewModel.local
            val target = chat.target.collectAsState().value
            if (target.type == com.ismartcoding.plain.features.chat.data.ChatTargetType.PEER) com.ismartcoding.plain.ui.chat.PeerInfoPage(navController, chat.target, { chat.clearAllMessages() }, localPeers)
            else com.ismartcoding.plain.ui.chat.ChannelInfoPage(navController, chat, localPeers, localChannels)
        } else com.ismartcoding.plain.ui.chat.ChatInfoPageRoute(navController, navController.getBackStackEntry<Routing.Home>())
    }
    contentDestination<Routing.Chat>(source) { entry ->
        val id = entry.toRoute<Routing.Chat>().id
        if (LocalContentSource.current is ContentSource.Local) com.ismartcoding.plain.ui.chat.ChatPage(navController, audioQueueVM, com.ismartcoding.plain.ui.chat.ChatViewModel.local, localPeers, localChannels, id)
        else com.ismartcoding.plain.ui.chat.ChatPageRoute(navController, id, navController.getBackStackEntry<Routing.Home>())
    }
    contentDestination<Routing.CastSession>(source) { com.ismartcoding.plain.ui.casting.CastSessionPage(navController) }
    contentDestination<Routing.Images>(source) { com.ismartcoding.plain.ui.images.ImagesPage(navController) }
    contentDestination<Routing.Videos>(source) { com.ismartcoding.plain.ui.videos.VideosPage(navController) }
    addAppsPages(navController, source)
    contentDestination<Routing.FeedEntries>(source) { entry ->
        val route = entry.toRoute<Routing.FeedEntries>()
        val device = LocalContentSource.current
        val owner = navController.getBackStackEntry<Routing.Home>()
        val tags = viewModel(viewModelStoreOwner = owner, key = "feed-tags") { TagsViewModel(device.tags) }
        val pager = viewModel(viewModelStoreOwner = owner, key = "feed-pager") { com.ismartcoding.plain.ui.feeds.FeedEntryPagerViewModel(device) }
        com.ismartcoding.plain.ui.feeds.FeedEntriesPage(navController, route.feedId, tags, pager)
    }
    contentDestination<Routing.FeedEntry>(source) { entry ->
        val route = entry.toRoute<Routing.FeedEntry>()
        val device = LocalContentSource.current
        val owner = navController.getBackStackEntry<Routing.Home>()
        val tags = viewModel(viewModelStoreOwner = owner, key = "feed-tags") { TagsViewModel(device.tags) }
        val pager = viewModel(viewModelStoreOwner = owner, key = "feed-pager") { com.ismartcoding.plain.ui.feeds.FeedEntryPagerViewModel(device) }
        com.ismartcoding.plain.ui.feeds.FeedEntryPage(navController, route.id, tags, pager, feedEntryVM = viewModel { com.ismartcoding.plain.ui.feeds.FeedEntryViewModel(device) })
    }
    contentDestination<Routing.FeedSettings>(source) {
        val current = LocalContentSource.current
        com.ismartcoding.plain.ui.feeds.FeedSettingsPage(navController, viewModel { com.ismartcoding.plain.ui.feeds.FeedSettingsViewModel(current) })
    }
    contentDestination<Routing.FeedCatalog>(source) { com.ismartcoding.plain.ui.feeds.FeedCatalogPage(navController) }
    addDeviceAudioRoutes(navController, audioQueueVM, source)
}
