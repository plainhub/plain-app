package com.ismartcoding.plain.ui.page.feeds

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.feed.CatalogCategoryIds
import com.ismartcoding.plain.features.feed.CatalogFeed
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.ToastManager
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.models.FeedCatalogViewModel
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedCatalogPage(
    navController: NavHostController,
    feedsVM: FeedsViewModel = viewModel { FeedsViewModel() },
    catalogVM: FeedCatalogViewModel = viewModel { FeedCatalogViewModel() },
) {
    val feedsState by feedsVM.itemsFlow.collectAsState()
    val categoriesState by catalogVM.categories
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        feedsVM.loadAsync()
        catalogVM.loadAsync()
    }
    val subscribedUrls = remember(feedsState) { feedsState.map { it.url }.toSet() }
    val selectableUrls =
        remember(categoriesState, subscribedUrls) {
            categoriesState.flatMap { it.feeds }.map { it.url }.filterNot { it in subscribedUrls }
        }
    val allSelected = catalogVM.selectedUrls.isNotEmpty() && catalogVM.selectedUrls.size == selectableUrls.size

    AddFeedDialog(feedsVM)

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.feed_catalog),
                actions = {
                    if (selectableUrls.isNotEmpty()) {
                        PTopRightButton(
                            label = stringResource(if (allSelected) Res.string.unselect_all else Res.string.select_all),
                            click = {
                                if (allSelected) catalogVM.clearSelection() else catalogVM.selectAll(selectableUrls)
                            },
                        )
                    }
                },
            )
        },
        bottomBar = {
            val count = catalogVM.selectedUrls.size
            if (count > 0) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { catalogVM.clearSelection() }) {
                            Text(stringResource(Res.string.unselect_all))
                        }
                        PFilledButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.subscribe_n, count),
                            enabled = !catalogVM.subscribing.value,
                            isLoading = catalogVM.subscribing.value,
                            onClick = {
                                catalogVM.subscribeAsync(feedsVM) { added ->
                                    ToastManager.showSuccessToast(
                                        LocaleHelper.getStringF(Res.string.added_n_subscriptions, added),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        when {
            catalogVM.loadFailed.value && categoriesState.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TopSpace()
                    PFilledButton(text = stringResource(Res.string.retry), onClick = {
                        scope.launch(IODispatcher) { catalogVM.loadAsync() }
                    })
                }
            }
            categoriesState.isEmpty() -> {
                NoDataColumn(loading = catalogVM.loading.value, search = false)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
                ) {
                    item(key = "top") { TopSpace() }
                    item(key = "manual") {
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            PListItem(
                                modifier = Modifier.clickable { feedsVM.showAddDialog() },
                                title = stringResource(Res.string.add_rss_manually),
                                icon = Res.drawable.plus,
                            )
                        }
                    }
                    categoriesState.forEach { category ->
                        item(key = "header-" + category.id) {
                            CategoryHeader(stringResource(categoryTitleRes(category.id)))
                        }
                        item(key = "card-" + category.id) {
                            PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                                Column {
                                    category.feeds.forEach { feed ->
                                        CatalogFeedRow(
                                            feed = feed,
                                            subscribed = feed.url in subscribedUrls,
                                            selected = feed.url in catalogVM.selectedUrls,
                                            onClick = {
                                                if (feed.url !in subscribedUrls) catalogVM.toggle(feed.url)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "bottom") { BottomSpace(paddingValues) }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun CatalogFeedRow(
    feed: CatalogFeed,
    subscribed: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    PListItem(
        modifier = Modifier.clickable(enabled = !subscribed) { onClick() },
        enable = !subscribed,
        title = feed.name,
        subtitle = feed.site,
        value = if (subscribed) stringResource(Res.string.already_subscribed) else null,
        start = {
            Checkbox(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                checked = subscribed || selected,
                onCheckedChange = null,
            )
        },
    )
}

private fun categoryTitleRes(id: String): StringResource =
    when (id) {
        CatalogCategoryIds.TECH_CN -> Res.string.category_tech_cn
        CatalogCategoryIds.DEV_CN -> Res.string.category_dev_cn
        CatalogCategoryIds.TECH_EN -> Res.string.category_tech_en
        CatalogCategoryIds.NEWS_EN -> Res.string.category_news_en
        CatalogCategoryIds.READS -> Res.string.category_reads
        else -> Res.string.feed_catalog
    }
