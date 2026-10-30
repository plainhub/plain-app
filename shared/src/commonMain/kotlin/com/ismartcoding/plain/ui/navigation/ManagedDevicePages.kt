package com.ismartcoding.plain.ui.navigation

import com.ismartcoding.plain.rust.graphql.ContentSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.lib.extensions.isPdfFile
import com.ismartcoding.plain.lib.extensions.isTextFile
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ismartcoding.plain.features.tags.TagsApi
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.nav.navEnterTransition
import com.ismartcoding.plain.ui.nav.navExitTransition
import com.ismartcoding.plain.ui.nav.navPopEnterTransition
import com.ismartcoding.plain.ui.nav.navPopExitTransition
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.tools.ManagedDeviceViewModel
import com.ismartcoding.plain.ui.tools.ManagedError
import com.ismartcoding.plain.ui.tools.ToolsPage

internal fun NavGraphBuilder.addManagedDevicePages(appNavController: NavHostController, audioQueueVM: AudioQueueViewModel) {
    composable<Routing.ManagedTools> { owner ->
        val route = owner.toRoute<Routing.ManagedTools>()
        val manager = viewModel(viewModelStoreOwner = owner, key = "managed-device") { ManagedDeviceViewModel(route.peerId, route.publicKey) }
        val navController = rememberNavController()
        val state by manager.state.collectAsState()
        val entered = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(state.client, state.loading, route) {
            if (!entered.value && !state.loading && state.client != null && state.error == null) {
                entered.value = true
                launchSafe { openManagedSearchEntry(navController, route, ContentSource.Remote(state.client!!)) }
            }
        }
        NavHost(
            navController,
            startDestination = Routing.ManagedToolsRoot,
            enterTransition = { navEnterTransition(PRESENTED_ROUTES) },
            exitTransition = { navExitTransition(PRESENTED_ROUTES) },
            popEnterTransition = { navPopEnterTransition(PRESENTED_ROUTES) },
            popExitTransition = { navPopExitTransition(PRESENTED_ROUTES) },
        ) {
            composable<Routing.ManagedToolsRoot> {
                ToolsPage(navController, manager, onNavigateBack = { appNavController.navigateUp() })
            }
            addManagedContentRoutes(navController, audioQueueVM, owner, manager)
        }
    }
}

private fun NavGraphBuilder.addManagedContentRoutes(
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel,
    owner: NavBackStackEntry,
    manager: ManagedDeviceViewModel,
) {
    addMainFilePages(navController, audioQueueVM) { managedScope(manager) }
    addNotesPages(navController) {
        managedScope(manager)?.let { source ->
            val tagsApi = remember(source.client) { TagsApi(source.client.api) }
            val notes = viewModel(viewModelStoreOwner = owner, key = "notes") { NotesViewModel(source.client.notes, tagsApi) }
            val tags = viewModel(viewModelStoreOwner = owner, key = "note-tags") { TagsViewModel(tagsApi) }
            NotesNavigationState(notes, tags, source)
        }
    }
    val source: @Composable () -> ContentSource? = { managedScope(manager) }
    contentDestination<Routing.ChatInfo>(source) { com.ismartcoding.plain.ui.chat.ChatInfoPageRoute(navController, owner) }
    contentDestination<Routing.Chat>(source) { entry -> com.ismartcoding.plain.ui.chat.ChatPageRoute(navController, entry.toRoute<Routing.Chat>().id, owner) }
    contentDestination<Routing.CastSession>(source) { com.ismartcoding.plain.ui.casting.CastSessionPage(navController) }
    contentDestination<Routing.Images>(source) { com.ismartcoding.plain.ui.images.ImagesPage(navController) }
    contentDestination<Routing.Videos>(source) { com.ismartcoding.plain.ui.videos.VideosPage(navController) }
    addAppsPages(navController, source)
    contentDestination<Routing.FeedEntries>(source) { entry ->
        val route = entry.toRoute<Routing.FeedEntries>()
        val device = LocalContentSource.current as ContentSource.Remote
        val tags = viewModel(viewModelStoreOwner = owner, key = "feed-tags") { TagsViewModel(device.tags) }
        val pager = viewModel(viewModelStoreOwner = owner, key = "feed-pager") { com.ismartcoding.plain.ui.feeds.FeedEntryPagerViewModel(device) }
        com.ismartcoding.plain.ui.feeds.FeedEntriesPage(navController, route.feedId, tags, pager)
    }
    contentDestination<Routing.FeedEntry>(source) { entry ->
        val route = entry.toRoute<Routing.FeedEntry>()
        val device = LocalContentSource.current as ContentSource.Remote
        val tags = viewModel(viewModelStoreOwner = owner, key = "feed-tags") { TagsViewModel(device.tags) }
        val pager = viewModel(viewModelStoreOwner = owner, key = "feed-pager") { com.ismartcoding.plain.ui.feeds.FeedEntryPagerViewModel(device) }
        com.ismartcoding.plain.ui.feeds.FeedEntryPage(navController, route.id, tags, pager, feedEntryVM = viewModel { com.ismartcoding.plain.ui.feeds.FeedEntryViewModel(device) })
    }
    contentDestination<Routing.FeedSettings>(source) {
        val current = LocalContentSource.current
        com.ismartcoding.plain.ui.feeds.FeedSettingsPage(navController, viewModel { com.ismartcoding.plain.ui.feeds.FeedSettingsViewModel(current) })
    }
    contentDestination<Routing.FeedCatalog>(source) { com.ismartcoding.plain.ui.feeds.FeedCatalogPage(navController) }
    addManagedAudioRoutes(navController, owner, manager)
}

@Composable
internal fun managedScope(manager: ManagedDeviceViewModel): ContentSource.Remote? {
    val state by manager.state.collectAsState()
    when {
        state.error != null -> ManagedError(state.error!!)
        state.loading -> NoDataColumn(loading = true)
        else -> {
            val content = remember(state.client) { ContentSource.Remote(state.client!!) }
            return content
        }
    }
    return null
}

private suspend fun openManagedSearchEntry(navController: NavHostController, route: Routing.ManagedTools, source: ContentSource.Remote) {
    if (route.entryFeature.isEmpty()) return
    when (val feature = AppFeatureType.valueOf(route.entryFeature)) {
        AppFeatureType.NOTES -> navController.navigate(Routing.NoteDetail(route.entryId))
        AppFeatureType.FEEDS -> navController.navigate(Routing.FeedEntry(route.entryId))
        AppFeatureType.APPS -> navController.navigate(Routing.AppDetails(route.entryId))
        AppFeatureType.FILES -> navController.navigate(Routing.Files(route.entryPath))
        AppFeatureType.CHAT -> navController.navigate(Routing.Chat(route.entryId))
        AppFeatureType.DOCS -> when {
            route.entryPath.isTextFile() -> navController.navigateTextFile(route.entryPath, route.entryTitle, route.entryId)
            route.entryPath.isPdfFile() -> navController.navigatePdf(com.ismartcoding.plain.platform.files.fileToUriString(source.fileForOpen(route.entryPath, route.entryTitle)), route.entryTitle)
            else -> navController.navigateOtherFile(route.entryPath, route.entryTitle)
        }
        else -> error("Unsupported search destination $feature")
    }
}
