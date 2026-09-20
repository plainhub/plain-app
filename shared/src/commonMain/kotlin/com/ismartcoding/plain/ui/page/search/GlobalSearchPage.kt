package com.ismartcoding.plain.ui.page.search

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.MediaPreviewer
import com.ismartcoding.plain.platform.checkNotificationPermission
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.GlobalSearchAction
import com.ismartcoding.plain.ui.models.GlobalSearchDomain
import com.ismartcoding.plain.ui.models.GlobalSearchHit
import com.ismartcoding.plain.ui.models.GlobalSearchSource
import com.ismartcoding.plain.ui.models.GlobalSearchViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.globalSearchDomains
import com.ismartcoding.plain.ui.page.MainNavScaffold
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Slack-style global search tab: one query across notes, audio, photos,
 * videos, docs, files, feeds, chat messages and apps. Results render with
 * each domain's own list item component; the sticky type chips narrow scope.
 */
@Composable
fun GlobalSearchPage(
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel,
    viewModel: GlobalSearchViewModel,
    onTabSelected: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val domains = globalSearchDomains()
    val query = viewModel.queryText.value
    val domainScope = viewModel.domain.value

    // Live suggestions: re-arm the debounce on every keystroke; a submit or a
    // recent-term tap flips `submitted` before this fires, so it stays quiet.
    LaunchedEffect(viewModel.queryText.value) {
        if (query.isNotBlank()) {
            delay(300)
            if (!viewModel.submitted.value) {
                viewModel.search()
            }
        }
    }

    val onOpen: (GlobalSearchHit) -> Unit = { hit ->
        when (val action = hit.action) {
            is GlobalSearchAction.Navigate -> navController.navigate(action.route)
            is GlobalSearchAction.PlayAudio -> checkNotificationPermission(Res.string.audio_notification_prompt) {
                scope.launch(Dispatchers.Default) { audioPlaylistVM.playAsync(action.audio) }
            }
        }
    }

    // Fresh VMs (search-prefixed keys) so search rows never mutate the source pages' state.
    val rowContext = rememberGlobalSearchRowContext(
        navController = navController,
        audioPlaylistVM = audioPlaylistVM,
        audioVM = viewModel(key = "searchAudioVM") { AudioViewModel() },
        tagsVM = viewModel(key = "searchTagsVM") { TagsViewModel() },
        castVM = viewModel(key = "searchCastVM") { CastViewModel() },
        docsVM = viewModel(key = "searchDocsVM") { DocsViewModel() },
        feedEntriesVM = viewModel(key = "searchFeedEntriesVM") { FeedEntriesViewModel() },
    )

    // Tapping an image/video hit opens the shared fullscreen previewer seeded
    // with every loaded hit of that section, at the tapped index
    // (same plain open() flow as ShortcutMediaPreviewer: no source thumbnail
    // is registered with the transform layer, so openTransform cannot run).
    val onPreviewMedia: (GlobalSearchHit) -> Unit = { hit ->
        val domain = if (hit.source is GlobalSearchSource.Video) GlobalSearchDomain.VIDEOS else GlobalSearchDomain.IMAGES
        val previewItems = buildMediaPreviewItems(viewModel.domainStates[domain]?.hits?.value ?: emptyList())
            .ifEmpty { listOfNotNull(hit.previewItem()) }
        val index = previewItems.indexOfFirst { it.id == hit.previewItem()?.id }
        if (index >= 0) {
            MediaPreviewData.items = previewItems
            scope.launch { rowContext.previewerState.open(index) }
        }
    }

    MainNavScaffold(
        selectedIndex = 3,
        onTabSelected = onTabSelected,
        topBar = { GlobalSearchTopBar(viewModel, domainScope) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            // All scopes stay visible while searching so switching filters is always possible.
            GlobalSearchScopeChips(
                selected = domainScope,
                chips = listOf<GlobalSearchDomain?>(null) + domains,
                onSelect = viewModel::setDomain,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (query.isBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (domainScope != null) {
                        Box(Modifier.padding(16.dp)) {
                            PAlert(
                                description = stringResource(Res.string.search_scope_hint, stringResource(domainScope.labelRes)),
                                type = AlertType.INFO,
                            )
                        }
                    }
                    GlobalSearchRecentSection(
                        recent = viewModel.recentQueries.value,
                        onSearch = viewModel::searchFromRecent,
                        onRemove = viewModel::removeRecent,
                        onClear = viewModel::clearRecent,
                    )
                }
            } else {
                SearchResults(
                    viewModel = viewModel,
                    domains = domains,
                    rowContext = rowContext,
                    onOpen = onOpen,
                    onPreviewMedia = onPreviewMedia,
                )
            }
        }
    }

    MediaPreviewer(state = rowContext.previewerState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlobalSearchTopBar(
    viewModel: GlobalSearchViewModel,
    domainScope: GlobalSearchDomain?,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    val hint = if (domainScope == null) stringResource(Res.string.global_search_hint)
    else stringResource(Res.string.search_in_domain, stringResource(domainScope.labelRes))

    Column(Modifier.fillMaxWidth()) {
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
            inputField = {
                SearchBarDefaults.InputField(
                    query = viewModel.queryText.value,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = { viewModel.submit() },
                    expanded = false,
                    onExpandedChange = { },
                    placeholder = { Text(hint) },
                    leadingIcon = {
                        PIcon(
                            icon = Res.drawable.search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (viewModel.queryText.value.isNotEmpty()) {
                            PIconButton(
                                icon = Res.drawable.close,
                                contentDescription = stringResource(Res.string.clear_search_term),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            ) {
                                viewModel.onQueryChange("")
                            }
                        }
                    },
                )
            },
            expanded = false,
            onExpandedChange = { },
            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.cardBackgroundNormal),
        ) {
        }
    }
}
