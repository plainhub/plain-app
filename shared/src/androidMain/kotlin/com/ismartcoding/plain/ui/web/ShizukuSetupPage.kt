package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
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
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShizukuSetupPage(navController: NavHostController) {
    val step = rememberShizukuStep()
    val browser = LocalUriHandler.current
    PScaffold(topBar = {
        PTopAppBar(title = stringResource(Res.string.setup_shizuku), onNavigateBack = { navController.navigateUp() })
    }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
            TopSpace()
            Text(stringResource(step.hint))
            VerticalSpace(dp = 16.dp)
            PFilledButton(text = stringResource(step.action), modifier = Modifier.fillMaxWidth(), onClick = {
                try {
                    when (step) {
                        ShizukuStep.Install -> browser.openUri(if (Locale.current.language == "zh")
                            "https://shizuku.rikka.app/zh-hans/download/" else "https://shizuku.rikka.app/download/")
                        ShizukuStep.Start, ShizukuStep.Manage -> openShizuku()
                        ShizukuStep.Authorize -> ShizukuHelper.requestPermission()
                        ShizukuStep.Ready -> navController.navigateUp()
                    }
                } catch (error: Exception) {
                    DialogHelper.showMessage(error)
                }
            })
        }
    }
}
