package com.ismartcoding.plain.ui.page.apps

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DPackageDetail
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.base.rememberLifecycleEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private fun isFileShareable(file: File): Boolean {
    if (!file.exists() || !file.canRead()) return false
    val path = file.absolutePath
    if (path.startsWith("/apex/")) return false
    if (path.startsWith("/system/") || path.startsWith("/vendor/") || path.startsWith("/product/")) {
        return try { file.inputStream().use { it.read() }; true } catch (e: Exception) { false }
    }
    return true
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPage(navController: NavHostController, id: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var item by remember { mutableStateOf<DPackageDetail?>(null) }
    var isShareable by remember { mutableStateOf(true) }
    val lifecycleEvent = rememberLifecycleEvent()

    LaunchedEffect(lifecycleEvent) {
        if (lifecycleEvent == Lifecycle.Event.ON_RESUME && PackageHelper.isUninstalled(id)) navController.navigateUp()
    }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            item = PackageHelper.getPackageDetail(id)
            item?.let { isShareable = isFileShareable(File(it.path)) }
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = item?.name ?: "", actions = {
                if (isShareable) {
                    PIconButton(icon = R.drawable.share_2, contentDescription = stringResource(R.string.share),
                        tint = MaterialTheme.colorScheme.onSurface) { ShareHelper.shareFile(context, File(item?.path ?: "")) }
                }
            })
        },
        content = { paddingValues ->
            if (item == null) { NoDataColumn(loading = true); return@PScaffold }
            val pkg = item!!
            LazyColumn(Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item { AppPageHeader(navController = navController, item = pkg) }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(title = stringResource(R.string.source_directory), subtitle = pkg.appInfo.sourceDir ?: "")
                        PListItem(title = stringResource(R.string.data_directory), subtitle = pkg.appInfo.dataDir ?: "")
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(title = stringResource(R.string.app_size), value = pkg.size.formatBytes())
                        PListItem(title = "SDK", value = LocaleHelper.getStringF(R.string.sdk, "target", pkg.appInfo.targetSdkVersion, "min", pkg.appInfo.minSdkVersion))
                        PListItem(title = stringResource(R.string.installed_at), value = pkg.installedAt.formatDateTime())
                        PListItem(title = stringResource(R.string.updated_at), value = pkg.updatedAt.formatDateTime())
                    }
                }
                item { BottomSpace(paddingValues) }
            }
        },
    )
}
