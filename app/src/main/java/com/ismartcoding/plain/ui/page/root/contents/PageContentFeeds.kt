package com.ismartcoding.plain.ui.page.root.contents

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.feeds.FeedEntriesPage

@Composable
fun PageContentFeeds(
    navController: NavHostController,
    tagsVM: TagsViewModel,
    onOpenDrawer: () -> Unit,
    paddingValues: PaddingValues,
) {
    FeedEntriesPage(
        navController = navController,
        feedId = "",
        tagsVM = tagsVM,
        onOpenDrawer = onOpenDrawer,
    )
}
