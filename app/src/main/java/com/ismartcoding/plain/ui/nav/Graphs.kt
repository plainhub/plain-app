package com.ismartcoding.plain.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.feeds.FeedEntriesPage
import com.ismartcoding.plain.ui.page.feeds.FeedEntryPage
import com.ismartcoding.plain.ui.page.notes.NotePage
import com.ismartcoding.plain.ui.page.notes.NotesPage

fun NavGraphBuilder.feedEntriesGraph(navController: NavHostController) {
    navigation(
        startDestination = RouteName.FEED_ENTRIES.name,
        route = "feed_entries",
    ) {
        composable(
            "${RouteName.FEED_ENTRIES.name}?feedId={feedId}",
            arguments = listOf(navArgument("feedId") {
                nullable = true
                defaultValue = ""
                type = NavType.StringType
            }),
        ) {
            val feedId = it.arguments?.getString("feedId") ?: ""
            val tagsViewModel = it.sharedViewModel<TagsViewModel>(navController)
            FeedEntriesPage(navController, feedId, tagsViewModel = tagsViewModel)
        }

        routeDetail(RouteName.FEED_ENTRIES) { entry, id ->
            val tagsViewModel = entry.sharedViewModel<TagsViewModel>(navController)
            FeedEntryPage(navController, id, tagsViewModel = tagsViewModel)
        }
    }
}

fun NavGraphBuilder.notesGraph(navController: NavHostController) {
    navigation(
        startDestination = RouteName.NOTES.name,
        route = "notes",
    ) {
        composable(RouteName.NOTES.name) { entry ->
            val tagsViewModel = entry.sharedViewModel<TagsViewModel>(navController)
            val notesViewModel = entry.sharedViewModel<NotesViewModel>(navController)
            NotesPage(navController, viewModel = notesViewModel, tagsViewModel = tagsViewModel)
        }

        composable(
            "${RouteName.NOTES.name}/create?tagId={tagId}",
            arguments = listOf(navArgument("tagId") {
                nullable = true
                defaultValue = ""
                type = NavType.StringType
            }),
        ) { entry ->
            val tagId = entry.arguments?.getString("tagId") ?: ""
            val tagsViewModel = entry.sharedViewModel<TagsViewModel>(navController)
            val notesViewModel = entry.sharedViewModel<NotesViewModel>(navController)
            NotePage(navController, "", tagId, notesViewModel = notesViewModel, tagsViewModel = tagsViewModel)
        }

        routeDetail(RouteName.NOTES) { entry, id ->
            val tagsViewModel = entry.sharedViewModel<TagsViewModel>(navController)
            val notesViewModel = entry.sharedViewModel<NotesViewModel>(navController)
            NotePage(navController, id, "", notesViewModel = notesViewModel, tagsViewModel = tagsViewModel)
        }
    }
}


