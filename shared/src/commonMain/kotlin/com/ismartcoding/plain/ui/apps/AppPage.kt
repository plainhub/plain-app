package com.ismartcoding.plain.ui.apps
import com.ismartcoding.plain.ui.theme.PlainTheme

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
import com.ismartcoding.plain.platform.system.PackageDetails
import com.ismartcoding.plain.platform.system.formatDateTime
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.platform.system.getPackageDetail
import com.ismartcoding.plain.platform.system.isFileShareable
import com.ismartcoding.plain.platform.system.shareFileAs
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.apps.components.*
import com.ismartcoding.plain.ui.audio.components.*
import com.ismartcoding.plain.ui.notes.components.*
import com.ismartcoding.plain.ui.web.components.*
import com.ismartcoding.plain.ui.components.*
import com.ismartcoding.plain.ui.media.*
import com.ismartcoding.plain.ui.components.rememberLifecycleEvent
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2
import com.ismartcoding.plain.ui.resources.package2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPage(
    navController: NavHostController,
    id: String,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
) {
    if (com.ismartcoding.plain.platform.system.Capability.PACKAGES !in source.capabilities) {
        LaunchedEffect(source) { navController.navigateUp() }
        return
    }
    val scope = rememberCoroutineScope()
    var details by remember(id, source) { mutableStateOf<PackageDetails?>(null) }
    var remoteItem by remember(id, source) { mutableStateOf<com.ismartcoding.plain.models.Package?>(null) }
    var isShareable by remember(id, source) { mutableStateOf(false) }
    var loading by remember(id, source) { mutableStateOf(true) }
    val hasPermission = remember(id, source) { mutableStateOf(source is com.ismartcoding.plain.rust.graphql.ContentSource.Local || source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote && com.ismartcoding.plain.platform.permissions.Permission.QUERY_ALL_PACKAGES in source.info.permissions) }
    val lifecycleEvent = rememberLifecycleEvent()
    if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
        com.ismartcoding.plain.ui.permissions.RemotePermissionEffects(source, com.ismartcoding.plain.platform.permissions.Permission.QUERY_ALL_PACKAGES, hasPermission)
    }
    LaunchedEffect(id, source, lifecycleEvent, hasPermission.value) {
        if (lifecycleEvent != Lifecycle.Event.ON_RESUME || !hasPermission.value) return@LaunchedEffect
        loading = true
        try {
            if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) {
                details = getPackageDetail(id)
                isShareable = details?.let { isFileShareable(it.item.path) } == true
                if (details == null) navController.navigateUp()
            } else {
                remoteItem = source.packages.find(id)
                isShareable = remoteItem?.path?.isNotEmpty() == true
                if (remoteItem == null) navController.navigateUp()
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            DialogHelper.showMessage(error)
        } finally {
            loading = false
        }
    }
    val pkg = if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) details?.item else remoteItem
    PScaffold(
        topBar = {
            PTopAppBar(onNavigateBack = { navController.navigateUp() }, title = pkg?.name ?: id, actions = {
                if (hasPermission.value && isShareable && pkg != null) {
                    PIconButton(icon = UiRes.drawable.ui_drawable_share_2, contentDescription = stringResource(Res.string.share),
                        tint = MaterialTheme.colorScheme.onSurface) {
                        scope.launchSafe {
                            val name = "${pkg.name.replace(" ", "")}-${pkg.id}.apk"
                            shareFileAs(source.fileForOpen(pkg.path, name), name)
                        }
                    }
                }
            })
        },
        content = { paddingValues ->
            if (!hasPermission.value) {
                NeedPermissionColumn(UiRes.drawable.package2, com.ismartcoding.plain.platform.permissions.FeaturePermission(setOf(com.ismartcoding.plain.platform.permissions.Permission.QUERY_ALL_PACKAGES), com.ismartcoding.plain.platform.permissions.Permission.QUERY_ALL_PACKAGES))
                return@PScaffold
            }
            if (pkg == null) { NoDataColumn(loading = loading); return@PScaffold }
            LazyColumn(Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item { AppPageHeader(navController = navController, item = pkg, details = details, source = source) }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(title = stringResource(Res.string.source_directory), subtitle = pkg.path)
                        details?.let { PListItem(title = stringResource(Res.string.data_directory), subtitle = it.dataDir) }
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(title = stringResource(Res.string.app_size), value = pkg.size.formatBytes())
                        details?.let { PListItem(title = "SDK", value = LocaleHelper.getStringF(Res.string.sdk, it.targetSdkVersion, it.minSdkVersion)) }
                        PListItem(title = stringResource(Res.string.installed_at), value = pkg.installedAt.formatDateTime())
                        PListItem(title = stringResource(Res.string.updated_at), value = pkg.updatedAt.formatDateTime())
                    }
                }
                item { BottomSpace(paddingValues) }
            }
        },
    )
}
