package com.ismartcoding.plain.ui.page.apps


import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Outbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
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
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.lib.helpers.ShareHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.pkg.DPackageDetail
import com.ismartcoding.plain.features.pkg.PackageHelper
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.SharedViewModel
import com.ismartcoding.plain.ui.page.RouteName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPage(
    navController: NavHostController,
    sharedViewModel: SharedViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var item by remember { mutableStateOf<DPackageDetail?>(null) }
    var groupButtons by remember { mutableStateOf(listOf<GroupButton>()) }
    val id = navController.currentBackStackEntry?.arguments?.getString("id") ?: ""
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
            val buttons = mutableListOf<GroupButton>()
            if (PackageHelper.canLaunch(item!!.id)) {
                buttons.add(
                    GroupButton(Icons.AutoMirrored.Outlined.Launch, LocaleHelper.getString(R.string.launch)) {
                        try {
                            PackageHelper.launch(context, item?.id ?: "")
                        } catch (ex: Exception) {
                            DialogHelper.showMessage(ex)
                        }
                    }
                )
            }
            buttons.add(
                GroupButton(
                    Icons.Outlined.DeleteOutline, LocaleHelper.getString(R.string.uninstall)
                ) {
                    try {
                        PackageHelper.uninstall(context, item?.id ?: "")
                    } catch (ex: Exception) {
                        DialogHelper.showMessage(ex)
                    }
                }
            )
            buttons.add(
                GroupButton(Icons.Outlined.Settings, LocaleHelper.getString(R.string.settings)) {
                    try {
                        PackageHelper.viewInSettings(context, item?.id ?: "")
                    } catch (ex: Exception) {
                        DialogHelper.showMessage(ex)
                    }
                }
            )
            buttons.add(
                GroupButton(Icons.Outlined.Outbox, "Manifest") {
                    try {
                        coMain {
                            DialogHelper.showLoading()
                            sharedViewModel.textTitle.value = "Manifest"
                            sharedViewModel.textContent.value = withIO { ApkParsers.getManifestXml(item?.path ?: "") }
                            DialogHelper.hideLoading()
                            navController.navigate(RouteName.TEXT)
                        }
                    } catch (ex: Exception) {
                        DialogHelper.showMessage(ex)
                    }
                }
            )
            groupButtons = buttons
        }
    }

    PScaffold(
        navController,
        topBarTitle = item?.name ?: "",
        actions = {
            PIconButton(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.share),
                tint = MaterialTheme.colorScheme.onSurface,
            ) {
                ShareHelper.share(context, Uri.parse(item?.path ?: ""))
            }
        },
        content = {
            if (item == null) {
                NoDataColumn(loading = true)
                return@PScaffold
            }
            LazyColumn {
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
                                modifier =
                                Modifier
                                    .padding(bottom = 16.dp)
                                    .size(56.dp),
                                painter = rememberDrawablePainter(drawable = icon),
                                contentDescription = item!!.name,
                            )
                            SelectionContainer {
                                Text(
                                    text = item?.id ?: "",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            VerticalSpace(dp = 8.dp)
                            SelectionContainer {
                                Text(
                                    text = LocaleHelper.getStringF(
                                        R.string.version_name_with_code,
                                        "version_name", item?.version ?: "", "version_code", PackageInfoCompat.getLongVersionCode(item!!.packageInfo)
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(
                                Modifier
                                    .padding(start = 32.dp, end = 32.dp, top = 8.dp),
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
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally),
                                horizontalArrangement = Arrangement.spacedBy(32.dp),
                            ) {
                                groupButtons.forEach { button ->
                                    PIconTextActionButton(
                                        icon = button.icon,
                                        text = button.text,
                                        click = button.onClick,
                                    )
                                }
                            }
                            VerticalSpace(dp = 16.dp)
                            Subtitle(
                                text = stringResource(R.string.paths_directories),
                            )

                            PCard {
                                PListItem(
                                    title = stringResource(R.string.source_directory),
                                    desc = item?.appInfo?.sourceDir ?: "",
                                )
                                PListItem(
                                    title = stringResource(R.string.data_directory),
                                    desc = item?.appInfo?.dataDir ?: "",
                                )
                            }

                            VerticalSpace(dp = 16.dp)

                            Subtitle(
                                text = stringResource(R.string.more_info),
                            )

                            PCard {
                                PListItem(
                                    title = stringResource(R.string.app_size),
                                    desc = FormatHelper.formatBytes(item?.size ?: 0),
                                )
                                PListItem(
                                    title = "SDK",
                                    desc = LocaleHelper.getStringF(R.string.sdk, "target", item?.appInfo?.targetSdkVersion ?: "", "min", item?.appInfo?.minSdkVersion ?: ""),
                                )
                                PListItem(
                                    title = stringResource(R.string.installed_at),
                                    desc = item?.installedAt?.formatDateTime(),
                                )
                                PListItem(
                                    title = stringResource(R.string.updated_at),
                                    desc = item?.updatedAt?.formatDateTime(),
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
