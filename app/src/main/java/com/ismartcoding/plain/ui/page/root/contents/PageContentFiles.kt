package com.ismartcoding.plain.ui.page.root.contents

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.page.files.FilesPage

@Composable
fun PageContentFiles(
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel,
    onOpenDrawer: () -> Unit,
    paddingValues: PaddingValues,
) {
    FilesPage(
        navController = navController,
        audioPlaylistVM = audioPlaylistVM,
        onOpenDrawer = onOpenDrawer,
    )
}
