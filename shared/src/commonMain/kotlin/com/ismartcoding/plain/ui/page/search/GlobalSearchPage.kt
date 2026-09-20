package com.ismartcoding.plain.ui.page.search

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.checkNotificationPermission
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.GlobalSearchAction
import com.ismartcoding.plain.ui.models.GlobalSearchDomain
import com.ismartcoding.plain.ui.models.GlobalSearchViewModel
import com.ismartcoding.plain.ui.models.globalSearchDomains
import com.ismartcoding.plain.ui.page.MainNavScaffold
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Slack-style global search tab: one query across notes, audio, photos,
 * videos, docs, files, feeds, chat messages and apps. All results render on
 * this page grouped by type; the sticky type chips narrow the scope.
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

    val onOpen: (com.ismartcoding.plain.ui.models.GlobalSearchHit) -> Unit = { hit ->
        when (val action = hit.action) {
            is GlobalSearchAction.Navigate -> navController.navigate(action.route)
            is GlobalSearchAction.PlayAudio -> checkNotificationPermission(Res.string.audio_notification_prompt) {
                scope.launch(Dispatchers.Default) { audioPlaylistVM.playAsync(action.audio) }
            }
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
            val chipDomains: List<GlobalSearchDomain?> = if (query.isBlank()) {
                listOf(null) + domains
            } else {
                val hitDomains = domains.filter { (viewModel.domainStates[it]?.total?.intValue ?: 0) > 0 }
                val list = mutableListOf<GlobalSearchDomain?>(null)
                list += hitDomains
                if (domainScope != null && domainScope !in hitDomains) list += domainScope
                list
            }
            GlobalSearchScopeChips(
                selected = domainScope,
                chips = chipDomains,
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
                    onOpen = onOpen,
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    viewModel: GlobalSearchViewModel,
    domains: List<GlobalSearchDomain>,
    onOpen: (com.ismartcoding.plain.ui.models.GlobalSearchHit) -> Unit,
) {
    val query = viewModel.queryText.value.trim()
    val domainScope = viewModel.domain.value
    val sectionDomains = domainScope?.let { listOf(it) } ?: domains
    val anyLoading = sectionDomains.any { viewModel.domainStates[it]?.loading?.value == true }
    val totalAll = sectionDomains.sumOf { viewModel.domainStates[it]?.total?.intValue ?: 0 }

    if (viewModel.searching.value && anyLoading && totalAll == 0) {
        NoDataColumn(loading = true)
        return
    }
    if (!viewModel.searching.value && viewModel.empty.value) {
        EmptyResults(viewModel, domainScope)
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        sectionDomains.forEach { d ->
            val state = viewModel.domainStates[d] ?: return@forEach
            val total = state.total.intValue
            if (total == 0 && !state.loading.value) return@forEach

            item(key = "header_${d.name}") {
                SectionHeader(d, total, state.loading.value)
            }
            val topN = if (d == GlobalSearchDomain.NOTES || d == GlobalSearchDomain.CHAT) GlobalSearchViewModel.SUGGEST_ROWS_WITH_SNIPPET else GlobalSearchViewModel.SUGGEST_ROWS
            val shown = if (viewModel.submitted.value) state.hits.value else state.hits.value.take(topN)
            items(shown, key = { it.key }) { hit ->
                GlobalSearchHitRow(hit = hit, query = query, onOpen = onOpen)
            }
            if (!viewModel.submitted.value && total > topN) {
                item(key = "viewall_${d.name}") {
                    SectionFooterButton(stringResource(Res.string.view_all_n, total)) {
                        viewModel.viewAllOfType(d)
                    }
                }
            } else if (viewModel.submitted.value && state.loaded.intValue < total) {
                item(key = "more_${d.name}") {
                    SectionFooterButton(
                        stringResource(Res.string.show_more_n, state.loaded.intValue, total),
                    ) {
                        viewModel.loadMore(d)
                    }
                }
            }
        }
        item(key = "bottomSpace") { BottomSpace() }
    }
}

@Composable
private fun SectionHeader(domain: GlobalSearchDomain, total: Int, loading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PIcon(icon = domain.iconRes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(domain.labelRes),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = if (loading && total == 0) "…" else total.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionFooterButton(text: String, onClick: () -> Unit) {
    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)) {
        POutlinedButton(
            text = text,
            onClick = onClick,
        )
    }
}

@Composable
private fun EmptyResults(
    viewModel: GlobalSearchViewModel,
    domainScope: GlobalSearchDomain?,
) {
    val query = viewModel.queryText.value.trim()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 48.dp)
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.cardBackgroundNormal),
            contentAlignment = Alignment.Center,
        ) {
            PIcon(
                icon = Res.drawable.search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(Res.string.no_results_for, query),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(if (domainScope != null) Res.string.no_results_hint_scope else Res.string.no_results_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (domainScope != null) {
                POutlinedButton(
                    text = stringResource(Res.string.search_in_all_types),
                    onClick = { viewModel.setDomain(null) },
                )
            }
            POutlinedButton(
                text = stringResource(Res.string.clear_search_term),
                onClick = { viewModel.onQueryChange("") },
            )
        }
        GlobalSearchRecentSection(
            recent = viewModel.recentQueries.value.take(4),
            onSearch = viewModel::searchFromRecent,
            onRemove = viewModel::removeRecent,
            onClear = viewModel::clearRecent,
        )
    }
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
