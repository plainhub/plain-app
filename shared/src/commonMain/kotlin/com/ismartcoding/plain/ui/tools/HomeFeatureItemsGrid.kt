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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.ui.base.PIconTextActionButton
import com.ismartcoding.plain.ui.components.reorderable.ReorderableItem
import com.ismartcoding.plain.ui.components.reorderable.rememberReorderableLazyGridState
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeFeatureItemsGrid(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    header: @Composable () -> Unit,
) {
    val featuresStr = Prefs.appHomeFeatures.collectAsStateValue()
    var enabledIds by remember(featuresStr) { mutableStateOf(Prefs.parseFeatures(featuresStr)) }
    val capabilities = getDeviceCapabilities()
    val allFeatureItems = remember(navController, capabilities) { FeatureItem.getList(navController) }
    val items = enabledIds.filter { it != AppFeatureType.CHAT.name }
        .mapNotNull { typeName -> allFeatureItems.find { it.type.name == typeName } }
    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        val fromIndex = enabledIds.indexOf(from.key as String)
        val toIndex = enabledIds.indexOf(to.key as String)
        if (fromIndex >= 0 && toIndex >= 0) {
            enabledIds = enabledIds.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            Prefs.set(PrefKeys.APP_HOME_FEATURES, Prefs.formatFeatures(enabledIds))
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
