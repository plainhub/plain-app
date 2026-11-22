package com.ismartcoding.plain.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.upgrade.LegacyDataUpgradeAction
import com.ismartcoding.plain.features.upgrade.LegacyDataUpgradeClient
import com.ismartcoding.plain.features.upgrade.LegacyDataUpgradeStatus
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LegacyDataUpgradePage(navController: NavHostController, startup: Boolean) {
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var status by remember { mutableStateOf<LegacyDataUpgradeStatus?>(null) }
    var action by remember { mutableStateOf<LegacyDataUpgradeAction?>(null) }
    var failure by remember { mutableStateOf(false) }
    val pending = status == LegacyDataUpgradeStatus.PENDING || status == LegacyDataUpgradeStatus.SKIPPED
    val mustChoose = startup && status != LegacyDataUpgradeStatus.IMPORTED && status != LegacyDataUpgradeStatus.CLEARED && !failure

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                status = LegacyDataUpgradeClient.execute(LegacyDataUpgradeAction.STATUS)
                failure = false
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                failure = true
            }
        }
    }

    val runAction: (LegacyDataUpgradeAction) -> Unit = { next ->
        if (action == null) {
            action = next
            failure = false
            scope.launch {
                try {
                    status = LegacyDataUpgradeClient.execute(next)
                    if (next == LegacyDataUpgradeAction.SKIP) navController.navigateUp()
                } catch (canceled: CancellationException) {
                    throw canceled
                } catch (_: Exception) {
                    failure = true
                } finally {
                    action = null
                }
            }
        }
    }

    PBackHandler(enabled = mustChoose || action != null) { }
    PScaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.legacy_data_upgrade),
                onNavigateBack = if (mustChoose || action != null) null else {
                    { navController.navigateUp() }
                },
            )
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier.padding(top = padding.calculateTopPadding()).padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { TopSpace() }
                val description = when {
                    pending -> Res.string.legacy_data_upgrade_prompt
                    status == LegacyDataUpgradeStatus.IMPORTED -> Res.string.legacy_data_upgrade_retained
                    status == LegacyDataUpgradeStatus.CLEARED -> Res.string.legacy_data_upgrade_cleared
                    status == LegacyDataUpgradeStatus.UNAVAILABLE -> Res.string.legacy_data_upgrade_unavailable
                    else -> null
                }
                description?.let { message ->
                    item { Text(stringResource(message), style = MaterialTheme.typography.bodyLarge) }
                }
                if (failure) {
                    item { Text(stringResource(Res.string.legacy_data_upgrade_error), color = MaterialTheme.colorScheme.error) }
                }
                if (pending) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PFilledButton(
                                text = stringResource(Res.string.ok),
                                onClick = { runAction(LegacyDataUpgradeAction.IMPORT) },
                                isLoading = action == LegacyDataUpgradeAction.IMPORT,
                                enabled = action != LegacyDataUpgradeAction.SKIP,
                            )
                            PTextButton(
                                text = stringResource(Res.string.legacy_data_upgrade_skip),
                                onClick = { runAction(LegacyDataUpgradeAction.SKIP) },
                                enabled = action == null,
                            )
                        }
                    }
                }
                if (status == LegacyDataUpgradeStatus.IMPORTED) {
                    item {
                        PTextButton(
                            text = stringResource(Res.string.legacy_data_upgrade_delete),
                            enabled = action == null,
                            onClick = {
                                scope.launch {
                                    confirmActionAsync(
                                        title = Res.string.legacy_data_upgrade_delete,
                                        message = Res.string.legacy_data_upgrade_delete_prompt,
                                        danger = true,
                                        callback = { runAction(LegacyDataUpgradeAction.CLEAR) },
                                    )
                                }
                            },
                        )
                    }
                }
                item { BottomSpace(padding) }
            }
        },
    )
}
