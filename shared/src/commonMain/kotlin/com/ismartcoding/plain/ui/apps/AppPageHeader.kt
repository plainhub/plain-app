package com.ismartcoding.plain.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.ismartcoding.plain.platform.system.PackageType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.i18n.settings
import com.ismartcoding.plain.platform.system.PackageDetails
import com.ismartcoding.plain.platform.system.canLaunchPackage
import com.ismartcoding.plain.platform.system.launchPackage
import com.ismartcoding.plain.platform.system.viewPackageInSettings
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIconTextActionButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.navigation.navigateText
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.code as ui_drawable_code
import com.ismartcoding.plain.ui.resources.delete_forever as ui_drawable_delete_forever
import com.ismartcoding.plain.ui.resources.settings as ui_drawable_settings
import com.ismartcoding.plain.ui.resources.square_arrow_out_up_right as ui_drawable_square_arrow_out_up_right
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppPageHeader(
    navController: NavHostController,
    item: com.ismartcoding.plain.models.Package,
    details: PackageDetails?,
    source: com.ismartcoding.plain.rust.graphql.ContentSource,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val localActions = source is com.ismartcoding.plain.rust.graphql.ContentSource.Local && canLaunchPackage(item.id)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val icon = if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
            com.ismartcoding.plain.ui.navigation.rememberContentResource(source, "pkgicon://${item.id}").value?.getOrNull()
        } else item
        AsyncImage(
            modifier = Modifier.padding(bottom = 16.dp).size(56.dp),
            model = icon,
            contentDescription = item.name,
        )
        SelectionContainer {
            Text(
                text = item.id, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        VerticalSpace(dp = 8.dp)
        SelectionContainer {
            Text(
                text = if (details != null) stringResource(Res.string.version_name_with_code, item.version, details.versionCode) else item.version,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp),
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SuggestionChip(onClick = {}, label = { Text(text = stringResource(if (item.type == PackageType.USER) Res.string.user_app else Res.string.system_app)) })
            if (details?.hasLargeHeap == true) {
                HorizontalSpace(dp = 8.dp)
                SuggestionChip(onClick = {}, label = { Text(text = stringResource(Res.string.large_heap)) })
            }
        }
        VerticalSpace(dp = 16.dp)
        if (localActions || source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                if (localActions) PIconTextActionButton(icon = UiRes.drawable.ui_drawable_square_arrow_out_up_right, text = stringResource(Res.string.launch), onClick = {
                    try { launchPackage(item.id) } catch (ex: Exception) { DialogHelper.showMessage(ex) }
                })
                if (localActions || item.type == PackageType.USER) PIconTextActionButton(icon = UiRes.drawable.ui_drawable_delete_forever, text = stringResource(Res.string.uninstall), onClick = {
                    scope.launchSafe { source.packages.uninstall(listOf(item.id)) }
                })
                if (localActions) PIconTextActionButton(icon = UiRes.drawable.ui_drawable_settings, text = stringResource(Res.string.settings), onClick = {
                    try { viewPackageInSettings(item.id) } catch (ex: Exception) { DialogHelper.showMessage(ex) }
                })
                PIconTextActionButton(icon = UiRes.drawable.ui_drawable_code, text = "Manifest", onClick = {
                    scope.launchSafe(onDone = { DialogHelper.hideLoading() }) {
                        DialogHelper.showLoading()
                        val content = source.packages.manifest(item.id)
                        navController.navigateText("Manifest", content, "xml")
                    }
                })
            }
        }
    }
}
