package com.ismartcoding.plain.ui.settings
import androidx.compose.foundation.layout.padding
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.platform.system.getLatestLogFilePath
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.components.ButtonType
import com.ismartcoding.plain.features.files.TextFileType
import com.ismartcoding.plain.platform.system.clearCacheAsync
import com.ismartcoding.plain.platform.system.clearLogsAsync
import com.ismartcoding.plain.platform.system.exportLogsAsync
import com.ismartcoding.plain.platform.system.getCacheSize
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.navigation.navigateTextFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
fun AboutLogsAndCacheCard(
    navController: NavHostController,
    scope: CoroutineScope,
    fileSize: Long,
    onFileSizeCleared: () -> Unit,
    cacheSize: Long,
    onCacheCleared: (Long) -> Unit,
) {
    val logsTitle = stringResource(Res.string.logs)
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(
            modifier = Modifier.clickable {
                navController.navigateTextFile(
                    getLatestLogFilePath(), logsTitle, "", TextFileType.APP_LOG
                )
            },
            title = stringResource(Res.string.logs),
            subtitle = fileSize.formatBytes(),
            separatedActions = true,
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PFilledButton(
                        text = stringResource(Res.string.share),
                        buttonSize = ButtonSize.SMALL, onClick = {
                            exportLogsAsync()
                        })
                    if (fileSize > 0L) {
                        POutlinedButton(
                            text = stringResource(Res.string.clear_logs),
                            buttonSize = ButtonSize.SMALL,
                            type = ButtonType.DANGER,
                            onClick = {
                                scope.launch {
                                    confirmActionAsync(
                                        Res.string.clear,
                                        Res.string.confirm_to_clear_logs,
                                        callback = {
                                            scope.launch {
                                                try {
                                                    clearLogsAsync()
                                                    onFileSizeCleared()
                                                } catch (canceled: CancellationException) { throw canceled }
                                                catch (error: Exception) { DialogHelper.showErrorMessage(error.toString()) }
                                            }
                                        },
                                        danger = true
                                    )
                                }
                            })
                    }
                }
            },
        )
        PListItem(
            title = stringResource(Res.string.local_cache),
            subtitle = cacheSize.formatBytes(),
            action = {
                POutlinedButton(
                    text = stringResource(Res.string.clear_cache),
                    type = ButtonType.DANGER,
                    buttonSize = ButtonSize.SMALL, onClick = {
                        scope.launch {
                            DialogHelper.showLoading()
                            try {
                                clearCacheAsync()
                                val newSize = getCacheSize()
                                onCacheCleared(newSize)
                                DialogHelper.showMessage(Res.string.local_cache_cleared)
                            } catch (canceled: CancellationException) {
                                throw canceled
                            } catch (error: Exception) {
                                DialogHelper.showErrorMessage(error.toString())
                            } finally {
                                DialogHelper.hideLoading()
                            }
                        }
                    })
            },
        )
    }
}
