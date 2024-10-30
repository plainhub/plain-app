package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.models.ISearchableViewModel
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.theme.cardContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : IData> ListSearchBar(
    viewModel: ISearchableViewModel<T>,
    onSearch: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { isWindowFocused ->
            if (isWindowFocused && viewModel.searchActive.value) {
                focusRequester.requestFocus()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchBar(
            modifier = Modifier
                .focusRequester(focusRequester),
            query = viewModel.queryText.value,
            onQueryChange = {
                viewModel.queryText.value = it
            },
            onSearch = onSearch,
            active = viewModel.searchActive.value,
            onActiveChange = {
                if (viewModel.searchActive.value != it) {
                    viewModel.searchActive.value = it
                    if (!viewModel.searchActive.value && viewModel.queryText.value.isEmpty()) {
                        viewModel.exitSearchMode()
                        onSearch("")
                    }
                }
            },
            placeholder = { Text(stringResource(id = R.string.search)) },
            leadingIcon = {
                PIconButton(
                    icon = R.drawable.arrow_left,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                ) {
                    if (!viewModel.searchActive.value || viewModel.queryText.value.isEmpty()) {
                        viewModel.exitSearchMode()
                        onSearch("")
                    } else {
                        viewModel.searchActive.value = false
                    }
                }
            },
            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.cardContainer()),
        ) {
        }
    }
}