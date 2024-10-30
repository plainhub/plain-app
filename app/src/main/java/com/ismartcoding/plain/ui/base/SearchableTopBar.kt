package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.models.ISearchableViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun <T : IData> SearchableTopBar(
    navController: NavHostController,
    viewModel: ISearchableViewModel<T>,
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    subtitle: String = "",
    scrollToTop: (() -> Unit)? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    onSearchAction: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    if (viewModel.showSearchBar.value) {
        ListSearchBar(
            viewModel = viewModel,
            onSearch = {
                viewModel.searchActive.value = false
                scope.launch(Dispatchers.IO) {
                    onSearchAction(it)
                }
            }
        )
        return
    }
    
    val topBarModifier = if (scrollToTop != null) {
        Modifier.combinedClickable(onClick = {}, onDoubleClick = {
            scope.launch {
                scrollToTop()
            }
        })
    } else {
        Modifier
    }
    
    PTopAppBar(
        modifier = topBarModifier,
        navController = navController,
        title = title,
        subtitle = subtitle,
        scrollBehavior = scrollBehavior,
        navigationIcon = navigationIcon,
        actions = actions
    )
} 