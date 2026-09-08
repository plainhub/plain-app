package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.features.feed.CatalogCategoryIds
import com.ismartcoding.plain.features.feed.CatalogFeed
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.add_rss_manually
import com.ismartcoding.plain.i18n.category_dev_cn
import com.ismartcoding.plain.i18n.category_news_en
import com.ismartcoding.plain.i18n.category_reads
import com.ismartcoding.plain.i18n.category_tech_cn
import com.ismartcoding.plain.i18n.category_tech_en
import com.ismartcoding.plain.i18n.feed_catalog
import com.ismartcoding.plain.i18n.plus
import com.ismartcoding.plain.i18n.retry
import com.ismartcoding.plain.i18n.subscribe
import com.ismartcoding.plain.i18n.unsubscribe
import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.models.FeedCatalogViewModel
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeedCatalogContent(
    feedsVM: FeedsViewModel,
    catalogVM: FeedCatalogViewModel,
    feedsState: List<DFeed>,
    paddingValues: PaddingValues,
    header: (@Composable () -> Unit)? = null,
) {
    val categoriesState by catalogVM.categories
    val scope = rememberCoroutineScope()
    val subscribedUrls = remember(feedsState) { feedsState.map { it.url }.toSet() }

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
                if (header != null) {
                    item(key = "header") { header() }
                }
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
                                        busy = feed.url in catalogVM.busyUrls,
                                        onToggle = {
                                            catalogVM.toggleSubscribeAsync(feed, feedsVM)
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
    busy: Boolean,
    onToggle: () -> Unit,
) {
    PListItem(
        title = feed.name,
        subtitle = feed.site,
        action = {
            POutlinedButton(
                text = stringResource(if (subscribed) Res.string.unsubscribe else Res.string.subscribe),
                onClick = onToggle,
                buttonSize = ButtonSize.SMALL,
                isLoading = busy,
                modifier = Modifier.padding(end = 8.dp),
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
