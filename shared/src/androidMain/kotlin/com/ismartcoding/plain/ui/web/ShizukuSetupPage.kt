package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.system.ShizukuHelper
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.cast
import com.ismartcoding.plain.ui.resources.message_circle
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShizukuSetupPage(navController: NavHostController) {
    val step = rememberShizukuStep()
    val browser = LocalUriHandler.current
    PScaffold(topBar = {
        PTopAppBar(title = stringResource(Res.string.adb_wireless_shizuku), onNavigateBack = { navController.navigateUp() })
    }) { padding ->
        val margin = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
            PSectionTitle(stringResource(Res.string.shizuku_benefits))
            PCard(modifier = margin) {
                PListItem(title = stringResource(Res.string.shizuku_control_title), subtitle = stringResource(Res.string.shizuku_control_hint), icon = UiRes.drawable.cast)
                PListItem(title = stringResource(Res.string.shizuku_sms_title), subtitle = stringResource(Res.string.shizuku_sms_hint), icon = UiRes.drawable.message_circle)
            }
            VerticalSpace(24.dp)
            if (step.action != null) PSectionTitle(stringResource(Res.string.shizuku_setup_heading))
            PCard(modifier = margin) {
                PListItem(title = stringResource(step.title), subtitle = stringResource(step.hint))
                if (step.action != null) PFilledButton(text = stringResource(step.action), modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), onClick = {
                    try {
                        when (step) {
                            ShizukuStep.Install -> browser.openUri(if (Locale.current.language == "zh")
                                "https://shizuku.rikka.app/zh-hans/download/" else "https://shizuku.rikka.app/download/")
                            ShizukuStep.Start, ShizukuStep.Manage -> openShizuku()
                            ShizukuStep.Authorize -> ShizukuHelper.requestPermission()
                            ShizukuStep.Ready -> Unit
                        }
                    } catch (error: Exception) {
                        DialogHelper.showMessage(error)
                    }
                })
            }
        }
    }
}
