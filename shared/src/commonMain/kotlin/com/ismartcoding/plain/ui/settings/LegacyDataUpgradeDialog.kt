package com.ismartcoding.plain.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ismartcoding.plain.features.upgrade.LegacyDataUpgradeAction
import com.ismartcoding.plain.features.upgrade.LegacyDataUpgradeClient
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.theme.dialogSheetBackground
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LegacyDataUpgradeDialog(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var upgrading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(Res.string.legacy_data_upgrade)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.legacy_data_upgrade_prompt))
                if (failed) {
                    Text(stringResource(Res.string.legacy_data_upgrade_error), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            PFilledButton(
                text = stringResource(Res.string.legacy_data_upgrade),
                isLoading = upgrading,
                onClick = {
                    if (!upgrading) {
                        upgrading = true
                        failed = false
                        scope.launch {
                            try {
                                LegacyDataUpgradeClient.execute(LegacyDataUpgradeAction.IMPORT)
                                onClose()
                            } catch (canceled: CancellationException) {
                                throw canceled
                            } catch (_: Exception) {
                                failed = true
                            } finally {
                                upgrading = false
                            }
                        }
                    }
                },
            )
        },
        dismissButton = {
            PTextButton(
                text = stringResource(Res.string.legacy_data_upgrade_skip),
                onClick = onClose,
                enabled = !upgrading,
            )
        },
    )
}
