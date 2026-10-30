package com.ismartcoding.plain.ui.navigation

import com.ismartcoding.plain.rust.graphql.ContentSource
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ismartcoding.plain.ui.notes.NotePage
import com.ismartcoding.plain.ui.notes.NotesPage
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel

internal data class NotesNavigationState(val notes: NotesViewModel, val tags: TagsViewModel, val source: ContentSource = ContentSource.Local)

internal fun NavGraphBuilder.addNotesPages(
    navController: NavHostController,
    state: @Composable () -> NotesNavigationState?,
) {
    composable<Routing.Notes> {
        state()?.let { value -> androidx.compose.runtime.CompositionLocalProvider(LocalContentSource provides value.source) {
            NotesPage(navController, tagsVM = value.tags, notesVM = value.notes)
        } }
    }
    composable<Routing.NotesCreate> { entry ->
        val route = entry.toRoute<Routing.NotesCreate>()
        state()?.let { value -> androidx.compose.runtime.CompositionLocalProvider(LocalContentSource provides value.source) {
            NotePage(navController, "", route.tagId, tagsVM = value.tags, notesVM = value.notes)
        } }
    }
    composable<Routing.NoteDetail> { entry ->
        val route = entry.toRoute<Routing.NoteDetail>()
        state()?.let { value -> androidx.compose.runtime.CompositionLocalProvider(LocalContentSource provides value.source) {
            NotePage(navController, route.id, "", tagsVM = value.tags, notesVM = value.notes)
        } }
    }
}
