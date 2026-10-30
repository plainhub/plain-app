package com.ismartcoding.plain.ui.tools

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.collectAsState
import com.ismartcoding.plain.ui.state.launchSafe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.ui.base.PIconTextActionButton
import com.ismartcoding.plain.ui.components.reorderable.ReorderableItem
import com.ismartcoding.plain.ui.components.reorderable.rememberReorderableLazyGridState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeFeatureItemsGrid(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: ToolsViewModel,
    header: @Composable () -> Unit,
) {
    val enabledIds by viewModel.enabledFeatures.collectAsState()
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val capabilities = source.capabilities
    val allFeatureItems = remember(navController, capabilities) { FeatureItem.getList(navController, capabilities, source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) }
    val items = enabledIds.filter { it != AppFeatureType.CHAT.name }
        .mapNotNull { typeName -> allFeatureItems.find { it.type.name == typeName } }
    if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
        FeatureItemsGrid(items, modifier, contentPadding, header = header)
        return
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        val fromIndex = enabledIds.indexOf(from.key as String)
        val toIndex = enabledIds.indexOf(to.key as String)
        if (fromIndex >= 0 && toIndex >= 0) {
            val reordered = enabledIds.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            scope.launchSafe { viewModel.save(reordered) }
        }
    }

    FeatureItemsGrid(items, modifier, contentPadding, gridState, header) { item ->
        ReorderableItem(state = reorderState, key = item.type.name) { _ ->
            FeatureItemAction(item, Modifier.longPressDraggableHandle())
        }
    }
}

@Composable
internal fun FeatureItemsGrid(
    features: List<FeatureItem>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    gridState: LazyGridState = rememberLazyGridState(),
    header: @Composable () -> Unit = {},
    itemContent: @Composable LazyGridItemScope.(FeatureItem) -> Unit = { FeatureItemAction(it) },
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp), state = gridState,
        modifier = modifier, contentPadding = contentPadding,
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        items(features, key = { it.type.name }) { item -> itemContent(item) }
    }
}

@Composable
private fun FeatureItemAction(item: FeatureItem, modifier: Modifier = Modifier) {
    PIconTextActionButton(
        icon = item.iconRes, text = stringResource(item.titleRes),
        modifier = modifier.fillMaxWidth().heightIn(min = 88.dp), onClick = item.click,
    )
}
