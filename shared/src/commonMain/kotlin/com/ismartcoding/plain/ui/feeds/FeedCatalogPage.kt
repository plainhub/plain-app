package com.ismartcoding.plain.ui.feeds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.models.Feed
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.feeds.FeedCatalogViewModel
import com.ismartcoding.plain.ui.feeds.FeedsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedCatalogPage(
    navController: NavHostController,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    feedsVM: FeedsViewModel = viewModel { FeedsViewModel(source) },
    catalogVM: FeedCatalogViewModel = viewModel { FeedCatalogViewModel(source) },
) {
    val feedsState by feedsVM.itemsFlow.collectAsState()
    LaunchedEffect(Unit) {
        feedsVM.loadAsync()
        catalogVM.loadAsync()
    }

    AddFeedDialog(feedsVM)

    PScaffold(
        topBar = {
            PTopAppBar(
                onNavigateBack = { navController.navigateUp() },
                title = stringResource(Res.string.feed_catalog),
            )
        },
    ) { paddingValues ->
        FeedCatalogContent(feedsVM, catalogVM, feedsState, paddingValues)
    }
}
