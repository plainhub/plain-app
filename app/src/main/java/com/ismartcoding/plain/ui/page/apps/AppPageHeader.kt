package com.ismartcoding.plain.ui.page.apps

import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.ismartcoding.lib.apk.ApkParsers
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DPackageDetail
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIconTextActionButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.navigateText

@Composable
fun AppPageHeader(
    navController: NavHostController,
    item: DPackageDetail,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val icon = packageManager.getApplicationIcon(item.appInfo)
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
                text = LocaleHelper.getStringF(
                    R.string.version_name_with_code, "version_name", item.version, "version_code",
                    PackageInfoCompat.getLongVersionCode(item.packageInfo)
                ),
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp),
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SuggestionChip(onClick = {}, label = { Text(text = stringResource(id = if (item.type == "user") R.string.user_app else R.string.system_app)) })
            if (item.hasLargeHeap) {
                HorizontalSpace(dp = 8.dp)
                SuggestionChip(onClick = {}, label = { Text(text = stringResource(id = R.string.large_heap)) })
            }
        }
        VerticalSpace(dp = 16.dp)
        if (PackageHelper.canLaunch(item.id)) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                PIconTextActionButton(icon = R.drawable.square_arrow_out_up_right, text = stringResource(R.string.launch), click = {
                    try { PackageHelper.launch(context, item.id) } catch (ex: Exception) { DialogHelper.showMessage(ex) }
                })
                PIconTextActionButton(icon = R.drawable.delete_forever, text = stringResource(R.string.uninstall), click = {
                    try { PackageHelper.uninstall(context, item.id) } catch (ex: Exception) { DialogHelper.showMessage(ex) }
                })
                PIconTextActionButton(icon = R.drawable.settings, text = stringResource(R.string.settings), click = {
                    try { PackageHelper.viewInSettings(context, item.id) } catch (ex: Exception) { DialogHelper.showMessage(ex) }
                })
                PIconTextActionButton(icon = R.drawable.code, text = "Manifest", click = {
                    coMain {
                        try {
                            DialogHelper.showLoading()
                            val content = withIO { ApkParsers.getManifestXml(item.path) }
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
