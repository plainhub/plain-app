package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.TextFileType
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.AppLogHelper
import com.ismartcoding.plain.preferences.DeveloperModePreference
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.navigateTextFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AboutLogsAndCacheCard(
    navController: NavHostController,
    context: android.content.Context,
    scope: CoroutineScope,
    fileSize: Long,
    onFileSizeCleared: () -> Unit,
    cacheSize: Long,
    onCacheCleared: (Long) -> Unit,
    developerMode: Boolean,
    onDeveloperModeChanged: (Boolean) -> Unit,
) {
    PCard {
        PListItem(
            modifier = Modifier.clickable {
                navController.navigateTextFile(
                    DiskLogFormatStrategy.getLogFolder(context) + "/latest.log", getString(R.string.logs), "", TextFileType.APP_LOG
                )
            },
            title = stringResource(R.string.logs),
            subtitle = fileSize.formatBytes(),
            separatedActions = true,
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PFilledButton(text = stringResource(R.string.share), buttonSize = ButtonSize.SMALL, onClick = {
                        AppLogHelper.export(context)
                    })
                    if (fileSize > 0L) {
                        PFilledButton(text = stringResource(R.string.clear_logs), buttonSize = ButtonSize.SMALL, onClick = {
                            DialogHelper.confirmToAction(R.string.confirm_to_clear_logs) {
                                val dir = File(DiskLogFormatStrategy.getLogFolder(context))
                                if (dir.exists()) dir.deleteRecursively()
                                onFileSizeCleared()
                            }
                        })
                    }
                }
            },
        )
        PListItem(
            title = stringResource(R.string.local_cache),
            subtitle = cacheSize.formatBytes(),
            action = {
                PFilledButton(text = stringResource(R.string.clear_cache), buttonSize = ButtonSize.SMALL, onClick = {
                    scope.launch {
                        DialogHelper.showLoading()
                        withIO {
                            AppHelper.clearCacheAsync(context)
                        }
                        coil3.SingletonImageLoader.get(context).memoryCache?.clear()
                        val newSize = AppHelper.getCacheSize(context)
                        DialogHelper.hideLoading()
                        DialogHelper.showMessage(R.string.local_cache_cleared)
                        onCacheCleared(newSize)
                    }
                })
            },
        )
        if (developerMode) {
            PListItem(title = stringResource(R.string.developer_mode)) {
                PSwitch(activated = developerMode) {
                    onDeveloperModeChanged(it)
                    scope.launch(Dispatchers.IO) {
                        DeveloperModePreference.putAsync(context, it)
                    }
                }
            }
        }
    }
}
