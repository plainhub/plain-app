package com.ismartcoding.plain.ui.page.apps

import com.ismartcoding.plain.i18n.*
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.ismartcoding.plain.helpers.coMain
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.DPackageInfo
import com.ismartcoding.plain.platform.canLaunchPackage
import com.ismartcoding.plain.platform.getApplicationIcon
import com.ismartcoding.plain.platform.getManifestXml
import com.ismartcoding.plain.platform.launchPackage
import com.ismartcoding.plain.platform.uninstallPackage
import com.ismartcoding.plain.platform.viewPackageInSettings
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIconTextActionButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.navigateText

@Composable
fun AppPageHeader(
    navController: NavHostController,
    item: DPackageInfo,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val icon = getApplicationIcon(item.id)
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
                text = stringRes(
                    Res.string.version_name_with_code, "version_name" to item.version, "version_code" to
                    item.versionCode
                ),
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp),
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SuggestionChip(onClick = {}, label = { Text(text = stringResource(if (item.type == "user") Res.string.user_app else Res.string.system_app)) })
            if (item.hasLargeHeap) {
                HorizontalSpace(dp = 8.dp)
                SuggestionChip(onClick = {}, label = { Text(text = stringResource(Res.string.large_heap)) })
            }
        }
        VerticalSpace(dp = 16.dp)
        if (canLaunchPackage(item.id)) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                PIconTextActionButton(icon = Res.drawable.square_arrow_out_up_right, text = stringResource(Res.string.launch), click = {
                    try { launchPackage(item.id) } catch (ex: Exception) { DialogHelper.showMessage(ex) }
                })
                PIconTextActionButton(icon = Res.drawable.delete_forever, text = stringResource(Res.string.uninstall), click = {
                    try { uninstallPackage(item.id) } catch (ex: Exception) { DialogHelper.showMessage(ex) }
                })
                PIconTextActionButton(icon = Res.drawable.settings, text = stringResource(Res.string.settings), click = {
                    try { viewPackageInSettings(item.id) } catch (ex: Exception) { DialogHelper.showMessage(ex) }
                })
                PIconTextActionButton(icon = Res.drawable.code, text = "Manifest", click = {
                    coMain {
                        try {
                            DialogHelper.showLoading()
                            val content = withIO { getManifestXml(item.path) }
                            DialogHelper.hideLoading()
                            navController.navigateText("Manifest", content, "xml")
                        } catch (ex: Exception) {
                            ex.printStackTrace(); DialogHelper.hideLoading(); DialogHelper.showErrorDialog(ex.toString())
                        }
                    }
                })
            }
        }
    }
}
