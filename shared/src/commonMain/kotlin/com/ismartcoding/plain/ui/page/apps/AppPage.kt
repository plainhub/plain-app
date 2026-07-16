package com.ismartcoding.plain.ui.page.apps

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.platform.DPackageInfo
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.getPackageDetail
import com.ismartcoding.plain.platform.isPackageInstalled
import com.ismartcoding.plain.platform.isFileShareable
import com.ismartcoding.plain.platform.shareFileAs
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.base.rememberLifecycleEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPage(navController: NavHostController, id: String) {
    val scope = rememberCoroutineScope()
    var item by remember { mutableStateOf<DPackageInfo?>(null) }
    var isShareable by remember { mutableStateOf(true) }
    val lifecycleEvent = rememberLifecycleEvent()

    LaunchedEffect(lifecycleEvent) {
        if (lifecycleEvent == Lifecycle.Event.ON_RESUME && !isPackageInstalled(id)) navController.navigateUp()
    }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            item = getPackageDetail(id)
            item?.let { isShareable = isFileShareable(it.path) }
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = item?.name ?: "", actions = {
                if (isShareable) {
                    PIconButton(icon = Res.drawable.share_2, contentDescription = stringResource(Res.string.share),
                        tint = MaterialTheme.colorScheme.onSurface) {
                        item?.let { pkg ->
                            shareFileAs(pkg.path, "${pkg.name.replace(" ", "")}-${pkg.id}.apk")
                        }
                    }
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
                        PListItem(title = stringResource(Res.string.source_directory), subtitle = pkg.sourceDir)
                        PListItem(title = stringResource(Res.string.data_directory), subtitle = pkg.dataDir)
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(title = stringResource(Res.string.app_size), value = pkg.size.formatBytes())
                        PListItem(title = "SDK", value = LocaleHelper.getStringF(Res.string.sdk, "target", pkg.targetSdkVersion, "min", pkg.minSdkVersion))
                        PListItem(title = stringResource(Res.string.installed_at), value = pkg.installedAt.formatDateTime())
                        PListItem(title = stringResource(Res.string.updated_at), value = pkg.updatedAt.formatDateTime())
                    }
                }
                item { BottomSpace(paddingValues) }
            }
        },
    )
}
