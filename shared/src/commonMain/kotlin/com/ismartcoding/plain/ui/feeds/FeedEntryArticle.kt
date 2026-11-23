package com.ismartcoding.plain.ui.feeds

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.extensions.timeAgo
import com.ismartcoding.plain.models.Feed
import com.ismartcoding.plain.models.FeedEntry
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.models.TagRelation
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.ui.markdown.compose.Markdown
import com.ismartcoding.plain.ui.markdown.compose.MarkdownElement
import com.ismartcoding.plain.ui.markdown.model.State
import com.ismartcoding.plain.ui.markdown.model.rememberMarkdownState
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.editor.markdown.rememberMarkdownImageTransformer
import com.ismartcoding.plain.ui.editor.markdown.rememberMarkdownTextConfig
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.features.web.WebHelper
import com.ismartcoding.plain.ui.navigation.navigateText
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun FeedEntryArticle(
    pageId: String,
    pagerVM: FeedEntryPagerViewModel,
    tagsState: List<Tag>,
    tagsMapState: Map<String, List<TagRelation>>,
    scrollState: LazyListState,
    scrollBehavior: TopAppBarScrollBehavior,
    fontScale: Float,
    feedEntryVM: FeedEntryViewModel,
    isCurrent: Boolean,
    topRefreshLayoutState: RefreshLayoutState,
    previewerState: MediaPreviewerState,
    paddingValues: PaddingValues,
    navController: NavHostController,
    scope: CoroutineScope,
) {
    val entryState = remember(pageId) { mutableStateOf<FeedEntry?>(null) }
    val feedState = remember(pageId) { mutableStateOf<Feed?>(null) }
    LaunchedEffect(pageId) {
        val m = pagerVM.entryAsync(pageId) ?: return@LaunchedEffect
        entryState.value = m
        feedState.value = pagerVM.feedAsync(m.feedId)
    }
    val m = entryState.value ?: return

    // Page recomposition precedes the page-change effect, so the shared content must still belong to this entry.
    val contentOverride = if (isCurrent && feedEntryVM.item.value?.id == pageId) feedEntryVM.content.value else ""
    val content = contentOverride.ifEmpty { pagerVM.cachedContent(pageId).ifEmpty { m.description } }
    val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()

    val density = LocalDensity.current
    val config = rememberMarkdownTextConfig()
    // Retain parsed blocks while replacement content loads to avoid a blank flash.
    val markdownState = rememberMarkdownState(content, retainState = true)
    val mdState by markdownState.state.collectAsState()

    val articleBody: @Composable (State.Success?, Modifier) -> Unit = { success, modifier ->
        SelectionContainer {
            LazyColumn(
                modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = scrollState,
            ) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 8.dp).clip(MaterialTheme.shapes.medium).combinedClickable(onDoubleClick = { navController.navigateText("JSON", jsonEncode(m, pretty = true), "json") }, onClick = { WebHelper.open(m.url) })) {
                        Text(text = m.title, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold))
                    }
                }
                item {
                    VerticalSpace(dp = 8.dp)
                    val tags = tagsState.filter { tagIds.contains(it.id) }
                    FlowRow(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = arrayOf(feedState.value?.name ?: "", m.author, m.publishedAt.timeAgo()).filter { it.isNotEmpty() }.joinToString(" \u00b7 "), style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        tags.forEach { tag -> Text(text = AnnotatedString("#" + tag.name), modifier = Modifier.wrapContentHeight().align(Alignment.Bottom), style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)) }
                    }
                    VerticalSpace(dp = 16.dp)
                }
                if (success != null) {
                    items(success.node.children.size, contentType = { "MarkdownBlock" }) { index ->
                        Box(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            MarkdownElement(success.node.children[index], config.components, success.content)
                        }
                    }
                }
                if (isCurrent && content == m.description && !m.isFullContent && topRefreshLayoutState.refreshContentState.value == RefreshContentState.Finished) {
                    item {
                        // Use the base density so article text scaling does not truncate the button label.
                        CompositionLocalProvider(LocalDensity provides density) {
                            VerticalSpace(dp = 32.dp)
                            if (feedEntryVM.fetchingContent.value) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp) } }
                            else {
                                POutlinedButton(
                                    text = stringResource(Res.string.load_full_content),
                                    modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN).fillMaxWidth(),
                                    enabled = !feedEntryVM.fetchingContent.value,
                                    onClick = {
                                        feedEntryVM.item.value?.let { entry ->
                                            scope.launchSafe(
                                                onError = { DialogHelper.showErrorDialog(it.toString()) },
                                                onDone = {
                                                    if (feedEntryVM.item.value?.id == entry.id) feedEntryVM.fetchingContent.value = false
                                                },
                                            ) {
                                                feedEntryVM.fetchingContent.value = true
                                                val content = pagerVM.fetchContentAsync(entry)
                                                if (feedEntryVM.item.value?.id == entry.id) feedEntryVM.content.value = content
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                item { BottomSpace(paddingValues) }
            }
        }
    }

    CompositionLocalProvider(LocalDensity provides Density(density = density.density, fontScale = density.fontScale * fontScale)) {
        Markdown(
            state = mdState,
            colors = config.colors,
            typography = config.typography,
            modifier = Modifier,
            padding = config.padding,
            dimens = config.dimens,
            extendedSpans = config.extendedSpans,
            imageTransformer = rememberMarkdownImageTransformer(mdState, previewerState),
            components = config.components,
            loading = { articleBody(null, it) },
            success = { s, _, modifier -> articleBody(s, modifier) },
        )
    }
}
