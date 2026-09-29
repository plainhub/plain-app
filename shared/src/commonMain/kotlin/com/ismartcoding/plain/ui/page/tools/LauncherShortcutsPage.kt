package com.ismartcoding.plain.ui.page.tools

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.preferences.LauncherShortcutsPreference
import com.ismartcoding.plain.preferences.appDataStore
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.reorderable.ReorderableItem
import com.ismartcoding.plain.ui.base.reorderable.rememberReorderableLazyListState
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.LauncherShortcutTools
import com.ismartcoding.plain.ui.page.home.DisabledFeatureCard
import com.ismartcoding.plain.ui.page.home.EnabledFeatureCard
import com.ismartcoding.plain.ui.page.home.FeatureItem
import com.ismartcoding.plain.platform.publishLauncherShortcuts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherShortcutsPage(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val itemsByType = remember {
        LauncherShortcutTools.ALL.associateWith { LauncherShortcutTools.featureItem(it) }
    }

    val stored = remember {
        appDataStore.dataFlow.map { LauncherShortcutsPreference.get(it) }
    }.collectAsStateValue(initial = LauncherShortcutsPreference.default)

    // Selected list order = shortcut display order (rank).
    var selected by remember(stored) {
        mutableStateOf(LauncherShortcutsPreference.selected(stored))
    }

    fun persist(newTools: List<AppFeatureType>) {
        selected = newTools
        scope.launch(Dispatchers.Default) {
            LauncherShortcutsPreference.putAsync(LauncherShortcutsPreference.formatList(newTools))
            publishLauncherShortcuts(newTools)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        val fromIdx = selected.indexOfFirst { it.name == fromKey }
        val toIdx = selected.indexOfFirst { it.name == toKey }
        if (fromIdx >= 0 && toIdx >= 0) {
            persist(selected.toMutableList().apply { add(toIdx, removeAt(fromIdx)) })
        }
    }

    val disabledTypes = remember(selected) {
        LauncherShortcutTools.ALL.filter { it !in selected }
    }

    var animateItems by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(300); animateItems = true }

    PScaffold(
        topBar = {
            PTopAppBar(onNavigateBack = { navController.navigateUp() }, title = stringResource(Res.string.launcher_shortcuts))
        },
        content = { paddingValues ->
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth().padding(top = paddingValues.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item(key = "hint") {
                    Text(
                        text = stringResource(Res.string.launcher_shortcuts_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    )
                    Text(
                        text = stringResource(Res.string.drag_number_to_reorder_list),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 24.dp, end = 16.dp, bottom = 8.dp),
                    )
                }
                itemsIndexed(selected, key = { _, type -> type.name }) { index, type ->
                    val feature = itemsByType[type] ?: return@itemsIndexed
                    ReorderableItem(
                        reorderableLazyListState, key = type.name,
                        animateItemModifier = if (animateItems) Modifier.animateItem() else Modifier,
                    ) {
                        EnabledFeatureCard(
                            feature = feature,
                            index = index,
                            onDisable = { persist(selected.filter { it != type }) },
                        )
                    }
                }
                items(disabledTypes, key = { "dis_${it.name}" }) { type ->
                    val feature = itemsByType[type] ?: return@items
                    DisabledFeatureCard(
                        feature = feature,
                        onEnable = {
                            if (selected.size >= LauncherShortcutTools.MAX_SELECTED) {
                                DialogHelper.showMessage(Res.string.launcher_shortcuts_limit)
                                return@DisabledFeatureCard
                            }
                            persist(selected + type)
                        },
                    )
                }
                item { BottomSpace() }
            }
        },
    )
}

