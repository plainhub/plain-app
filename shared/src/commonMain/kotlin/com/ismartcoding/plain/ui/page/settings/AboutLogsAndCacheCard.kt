package com.ismartcoding.plain.ui.page.settings

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
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.DiskLogFormatStrategy
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.enums.TextFileType
import com.ismartcoding.plain.platform.clearCacheAsync
import com.ismartcoding.plain.platform.clearImageMemoryCache
import com.ismartcoding.plain.platform.clearLogsAsync
import com.ismartcoding.plain.platform.exportLogsAsync
import com.ismartcoding.plain.platform.getCacheSize
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.navigateTextFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
    PCard {
        PListItem(
            modifier = Modifier.clickable {
                navController.navigateTextFile(
                    DiskLogFormatStrategy.getLogFolder() + "/latest.log", logsTitle, "", TextFileType.APP_LOG
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
                                DialogHelper.confirmToAction(Res.string.confirm_to_clear_logs) {
                                    clearLogsAsync()
                                    onFileSizeCleared()
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
                            withIO {
                                clearCacheAsync()
                            }
                            clearImageMemoryCache()
                            val newSize = getCacheSize()
                            DialogHelper.hideLoading()
                            DialogHelper.showMessage(Res.string.local_cache_cleared)
                            onCacheCleared(newSize)
                        }
                    })
            },
        )
    }
}
