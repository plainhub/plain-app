package com.ismartcoding.plain.ui.page.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.models.GlobalSearchDomain
import com.ismartcoding.plain.ui.models.GlobalSearchViewModel
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import org.jetbrains.compose.resources.stringResource

/** Search input bar of the global search tab, with once-per-session auto focus. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GlobalSearchTopBar(
    viewModel: GlobalSearchViewModel,
    domainScope: GlobalSearchDomain?,
) {
    val focusRequester = remember { FocusRequester() }
    // Auto-focus once per session when entering empty; returning from a
    // viewer page (TextFilePage, FeedEntry, …) re-enters composition and
    // must not pop the keyboard back up.
    var autoFocusDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoFocusDone && viewModel.queryText.value.isEmpty()) {
            autoFocusDone = true
            focusRequester.requestFocus()
        }
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
