package com.ismartcoding.plain.ui.page.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.preferences.HomeFeaturesPreference
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.reorderable.ReorderableItem
import com.ismartcoding.plain.ui.base.reorderable.rememberReorderableLazyListState
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.theme.secondaryTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeaturesSelectionPage(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allFeatureItems = remember { FeatureItem.getList(navController) }

    val featuresStr = remember {
        context.dataStore.dataFlow.map { HomeFeaturesPreference.get(it) }
    }.collectAsStateValue(initial = HomeFeaturesPreference.default)

    var enabledIds by remember(featuresStr) {
        mutableStateOf(
            HomeFeaturesPreference.parseList(featuresStr.ifEmpty { HomeFeaturesPreference.default })
        )
    }

    fun persist(newList: List<String>) {
        enabledIds = newList
        scope.launch(Dispatchers.IO) {
            HomeFeaturesPreference.putAsync(context, HomeFeaturesPreference.formatList(newList))
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        val fromIdx = enabledIds.indexOf(fromKey)
        val toIdx = enabledIds.indexOf(toKey)
        if (fromIdx >= 0 && toIdx >= 0) {
            persist(enabledIds.toMutableList().apply { add(toIdx, removeAt(fromIdx)) })
        }
    }

    val disabledItems = remember(enabledIds) {
        allFeatureItems.filter { it.type.name !in enabledIds }
    }

    var animateItems by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(300); animateItems = true }

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = stringResource(R.string.customize_home_features))
        },
        content = { paddingValues ->
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth().padding(top = paddingValues.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item(key = "hint") {
                    Text(
                        text = stringResource(R.string.drag_number_to_reorder_list),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondaryTextColor,
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    )
                }
                itemsIndexed(enabledIds, key = { _, id -> id }) { index, featureId ->
                    val feature = allFeatureItems.find { it.type.name == featureId }
                        ?: return@itemsIndexed
                    ReorderableItem(
                        reorderableLazyListState, key = featureId,
                        animateItemModifier = if (animateItems) Modifier.animateItem() else Modifier,
                    ) {
                        EnabledFeatureCard(
                            feature = feature,
                            index = index,
                            onDisable = { persist(enabledIds.filter { it != featureId }) },
                        )
                    }
                }
                items(disabledItems, key = { "dis_${it.type.name}" }) { feature ->
                    DisabledFeatureCard(
                        feature = feature,
                        onEnable = { persist(enabledIds + feature.type.name) },
                    )
                }
            }
        },
    )
}

