package com.ismartcoding.plain.ui.page.apps

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.PackageListItem
import com.ismartcoding.plain.ui.models.AppsViewModel
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.cardContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsSearchPage(
    navController: NavHostController,
    q: String
) {
    val viewModel: AppsViewModel = viewModel()
    val itemsState by viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var active by rememberSaveable {
        mutableStateOf(true)
    }
    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO { viewModel.loadAsync() }
                setRefreshState(RefreshContentState.Finished)
            }
        }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (viewModel.queryText.isEmpty()) {
            viewModel.queryText = q
        }
        if (active) {
            focusRequester.requestFocus()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchBar(
            modifier = Modifier
                .focusRequester(focusRequester),
            query = viewModel.queryText,
            onQueryChange = {
                viewModel.queryText = it
            },
            onSearch = {
                if (viewModel.queryText.isNotEmpty()) {
                    active = false
                    viewModel.showLoading.value = true
                    scope.launch(Dispatchers.IO) {
                        viewModel.loadAsync()
                    }
                }
            },
            active = active,
            onActiveChange = {
                if (active != it) {
                    active = it
                    if (!active && viewModel.queryText.isEmpty()) {
                        navController.popBackStack()
                    }
                }
            },
            placeholder = { Text(stringResource(id = R.string.search)) },
            leadingIcon = {
                PIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                ) {
                    if (!active || viewModel.queryText.isEmpty()) {
                        navController.popBackStack()
                    } else {
                        active = false
                    }
                }
            },
            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.cardContainer()),
        ) {
        }
        TopSpace()
        PullToRefresh(
            refreshLayoutState = topRefreshLayoutState,
        ) {
            if (itemsState.isNotEmpty()) {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                ) {
                    item {
                        TopSpace()
                    }
                    items(itemsState, key = {
                        it.id
                    }) {  m ->
                        PackageListItem(
                            item = m,
                            modifier = PlainTheme.getCardModifier(),
                            onClick = {
                                navController.navigate("${RouteName.APPS.name}/${m.id}")
                            }
                        )
                        VerticalSpace(dp = 8.dp)
                    }
                    item {
                        if (itemsState.isNotEmpty() && !viewModel.noMore.value) {
                            LaunchedEffect(Unit) {
                                scope.launch(Dispatchers.IO) {
                                    withIO { viewModel.moreAsync() }
                                }
                            }
                        }
                        LoadMoreRefreshContent(viewModel.noMore.value)
                        BottomSpace()
                    }
                }
            } else {
                NoDataColumn(loading = viewModel.showLoading.value, search = true)
            }
        }
    }
}

