package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.FeedCatalogViewModel
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.theme.green
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * First-run discovery mode for FeedEntriesPage: a short hero + the curated
 * catalog. Subscribing never leaves this screen; the bottom "start reading"
 * bar (once something is subscribed) is the only way out, so page switches
 * are always explicit, never driven by data changes. The hero/banner scrolls
 * away with the catalog; only the bottom bar stays pinned.
 */
@Composable
fun FeedDiscoveryContent(
    feedsVM: FeedsViewModel,
    catalogVM: FeedCatalogViewModel,
    feedsState: List<DFeed>,
    onStartReading: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            FeedCatalogContent(feedsVM, catalogVM, feedsState, PaddingValues(0.dp)) {
                DiscoveryHeader(feedsState)
            }
        }
        AnimatedVisibility(
            visible = feedsState.isNotEmpty(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            DiscoveryBottomBar(count = feedsState.size, onStartReading = onStartReading)
        }
    }
}

/**
 * Scrolling header of the discovery catalog: hero while nothing is
 * subscribed, success banner once it is. Sits inside the LazyColumn so it
 * scrolls away with the content.
 */
@Composable
private fun DiscoveryHeader(feedsState: List<DFeed>) {
    Column {
        AnimatedVisibility(visible = feedsState.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
            DiscoveryHero(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        AnimatedVisibility(visible = feedsState.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            DiscoveryBanner(count = feedsState.size)
        }
    }
}

@Composable
private fun DiscoveryHero(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.rss),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        VerticalSpace(dp = 12.dp)
        Text(stringResource(Res.string.discover_feeds_title), style = MaterialTheme.typography.titleMedium)
        VerticalSpace(dp = 8.dp)
        Text(
            stringResource(Res.string.discover_feeds_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DiscoveryBanner(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(MaterialTheme.colorScheme.green.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.check),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.green,
            modifier = Modifier.size(16.dp),
        )
        HorizontalSpace(dp = 8.dp)
        Text(
            stringResource(Res.string.subscribed_n_syncing, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.green,
        )
    }
}

@Composable
private fun DiscoveryBottomBar(count: Int, onStartReading: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.subscribed_n_feeds, count),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            PFilledButton(text = stringResource(Res.string.start_reading), onClick = onStartReading)
        }
    }
}

/**
 * Empty state for the entries list when feeds exist but no articles are
 * showing (not the discovery catalog — that would send users back the wrong
 * way; adding more feeds lives in the drawer).
 */
@Composable
fun EmptyArticlesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.rss),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
        VerticalSpace(dp = 16.dp)
        Text(stringResource(Res.string.no_articles), style = MaterialTheme.typography.titleMedium)
        VerticalSpace(dp = 8.dp)
        Text(
            stringResource(Res.string.no_articles_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
    }
}
