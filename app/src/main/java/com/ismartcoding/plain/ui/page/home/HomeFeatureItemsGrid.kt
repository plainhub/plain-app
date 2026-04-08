package com.ismartcoding.plain.ui.page.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.preferences.HomeFeaturesPreference
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.ui.base.reorderable.ReorderableItem
import com.ismartcoding.plain.ui.base.reorderable.rememberReorderableLazyStaggeredGridState
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun HomeFeatureItemsGrid(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val featuresStr = remember {
        context.dataStore.dataFlow.map { HomeFeaturesPreference.get(it) }
    }.collectAsStateValue(initial = HomeFeaturesPreference.default)

    var enabledIds by remember(featuresStr) {
        mutableStateOf(HomeFeaturesPreference.parseList(featuresStr.ifEmpty { HomeFeaturesPreference.default }))
    }

    fun persist(newList: List<String>) {
        enabledIds = newList
        scope.launch(Dispatchers.IO) {
            HomeFeaturesPreference.putAsync(context, HomeFeaturesPreference.formatList(newList))
        }
    }

    val allFeatureItems = remember { FeatureItem.getList(navController) }
    val items = remember(enabledIds) {
        enabledIds.mapNotNull { typeName -> allFeatureItems.find { it.type.name == typeName } }
    }

    val rowCount = (items.size + 1) / 2
    val gridHeight = if (rowCount > 0) (rowCount * 72 + (rowCount - 1) * 12).dp else 0.dp

    val gridState = rememberLazyStaggeredGridState()
    val reorderableState = rememberReorderableLazyStaggeredGridState(gridState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyStaggeredGridState
        val toKey = to.key as? String ?: return@rememberReorderableLazyStaggeredGridState
        val fromIdx = enabledIds.indexOf(fromKey)
        val toIdx = enabledIds.indexOf(toKey)
        if (fromIdx >= 0 && toIdx >= 0) {
            persist(enabledIds.toMutableList().apply { add(toIdx, removeAt(fromIdx)) })
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxWidth().height(gridHeight),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(items, key = { it.type.name }) { item ->
            ReorderableItem(reorderableState, key = item.type.name, animateItemModifier = Modifier) { _ ->
                Surface(
                    modifier = Modifier
                        .height(72.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .longPressDraggableHandle()
                        .clickable { item.click() },
                    color = MaterialTheme.colorScheme.cardBackgroundNormal,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = stringResource(item.titleRes),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(item.titleRes),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
