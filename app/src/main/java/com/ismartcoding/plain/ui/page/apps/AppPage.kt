package com.ismartcoding.plain.ui.page.apps


import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.ismartcoding.lib.apk.ApkParsers
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.data.DPackageDetail
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PIconTextActionButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.rememberLifecycleEvent
import com.ismartcoding.plain.ui.nav.navigateText
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Check if a file can be shared through the app's FileProvider
 */
private fun isFileShareable(file: File): Boolean {
    if (!file.exists() || !file.canRead()) {
        return false
    }
    
    val path = file.absolutePath
    // Files in /apex/ directory are modular system components and cannot be shared via FileProvider
    if (path.startsWith("/apex/")) {
        return false
    }
    
    // Files in other system directories may also be inaccessible
    if (path.startsWith("/system/") || path.startsWith("/vendor/") || path.startsWith("/product/")) {
        // Try to access the file to see if it's readable
        try {
            file.inputStream().use { it.read() }
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    return true
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPage(
    navController: NavHostController,
    id: String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var item by remember { mutableStateOf<DPackageDetail?>(null) }
    var isShareable by remember { mutableStateOf(true) }
    val lifecycleEvent = rememberLifecycleEvent()
    LaunchedEffect(lifecycleEvent) {
        if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
            if (PackageHelper.isUninstalled(id)) {
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            item = PackageHelper.getPackageDetail(id)
            // Check if the APK file can be shared
            item?.let { packageDetail ->
                isShareable = isFileShareable(File(packageDetail.path))
            }
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = item?.name ?: "",
                actions = {
                    if (isShareable) {
                        PIconButton(
                            icon = R.drawable.share_2,
                            contentDescription = stringResource(R.string.share),
                            tint = MaterialTheme.colorScheme.onSurface,
                        ) {
                            ShareHelper.shareFile(context, File(item?.path ?: ""))
                        }
                    }
                },
            )
        },
        content = { paddingValues ->
            if (item == null) {
                NoDataColumn(loading = true)
                return@PScaffold
            }
            LazyColumn(Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (item != null) {
                            val icon = packageManager.getApplicationIcon(item!!.appInfo)
                            Image(
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .size(56.dp),
                                painter = rememberDrawablePainter(drawable = icon),
                                contentDescription = item!!.name,
                            )
                            SelectionContainer {
                                Text(
                                    text = item?.id ?: "",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            VerticalSpace(dp = 8.dp)
                            SelectionContainer {
                                Text(
                                    text = LocaleHelper.getStringF(
                                        R.string.version_name_with_code, "version_name", item?.version ?: "", "version_code", PackageInfoCompat.getLongVersionCode(item!!.packageInfo)
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(
                                Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SuggestionChip(onClick = {}, label = {
                                    Text(text = stringResource(id = if (item?.type == "user") R.string.user_app else R.string.system_app))
                                })
                                if (item?.hasLargeHeap == true) {
                                    HorizontalSpace(dp = 8.dp)
                                    SuggestionChip(onClick = {}, label = {
                                        Text(text = stringResource(id = R.string.large_heap))
                                    })
                                }
                            }
                            VerticalSpace(dp = 16.dp)

                            Row(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                horizontalArrangement = Arrangement.spacedBy(32.dp),
                            ) {
                                if (item != null && PackageHelper.canLaunch(item!!.id)) {
                                    PIconTextActionButton(
                                        icon = R.drawable.square_arrow_out_up_right, text = stringResource(R.string.launch),
                                        click = {
                                            try {
                                                PackageHelper.launch(context, item?.id ?: "")
                                            } catch (ex: Exception) {
                                                DialogHelper.showMessage(ex)
                                            }
                                        })

                                    PIconTextActionButton(
                                        icon = R.drawable.delete_forever, text = stringResource(R.string.uninstall), click = {
                                            try {
                                                PackageHelper.uninstall(context, item?.id ?: "")
                                            } catch (ex: Exception) {
                                                DialogHelper.showMessage(ex)
                                            }

                                        })
                                    PIconTextActionButton(
                                        icon = R.drawable.settings, text = stringResource(R.string.settings), click = {
                                            try {
                                                PackageHelper.viewInSettings(context, item?.id ?: "")
                                            } catch (ex: Exception) {
                                                DialogHelper.showMessage(ex)
                                            }

                                        })
                                    PIconTextActionButton(
                                        icon = R.drawable.code, text = "Manifest", click = {
                                            coMain {
                                                try {
                                                    DialogHelper.showLoading()
                                                    val content = withIO { ApkParsers.getManifestXml(item?.path ?: "") }
                                                    DialogHelper.hideLoading()
                                                    navController.navigateText("Manifest", content, "xml")
                                                } catch (ex: Exception) {
                                                    ex.printStackTrace()
                                                    DialogHelper.hideLoading()
                                                    DialogHelper.showErrorDialog(ex.toString())
                                                }
                                            }

                                        })
                                }
                            }
                        }
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(
                            title = stringResource(R.string.source_directory),
                            subtitle = item?.appInfo?.sourceDir ?: "",
                        )
                        PListItem(
                            title = stringResource(R.string.data_directory),
                            subtitle = item?.appInfo?.dataDir ?: "",
                        )
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(
                            title = stringResource(R.string.app_size),
                            value = (item?.size ?: 0).formatBytes(),
                        )
                        PListItem(
                            title = "SDK",
                            value = LocaleHelper.getStringF(R.string.sdk, "target", item?.appInfo?.targetSdkVersion ?: "", "min", item?.appInfo?.minSdkVersion ?: ""),
                        )
                        PListItem(
                            title = stringResource(R.string.installed_at),
                            value = item?.installedAt?.formatDateTime(),
                        )
                        PListItem(
                            title = stringResource(R.string.updated_at),
                            value = item?.updatedAt?.formatDateTime(),
                        )
                    }
                }
                item {
                    BottomSpace()
                }
            }
        },
    )
}
