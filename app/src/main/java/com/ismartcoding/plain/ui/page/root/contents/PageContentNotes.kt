package com.ismartcoding.plain.ui.page.root.contents

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.notes.NotesPage

@Composable
fun PageContentNotes(
    navController: NavHostController,
    notesVM: NotesViewModel,
    tagsVM: TagsViewModel,
    onOpenDrawer: () -> Unit,
    paddingValues: PaddingValues,
) {
    NotesPage(
        navController = navController,
        notesVM = notesVM,
        tagsVM = tagsVM,
        onOpenDrawer = onOpenDrawer,
    )
}
