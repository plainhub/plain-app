package com.ismartcoding.plain.ui.page.devoptions
import com.ismartcoding.plain.preferences.*
import androidx.compose.foundation.layout.fillMaxWidth

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.preferences.LocalAdbToken
import com.ismartcoding.plain.preferences.WebSettingsProvider
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.ClipboardCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDevPage(
    navController: NavHostController,
    packageId: String,
    onResetToken: () -> Unit,
) {
    WebSettingsProvider {
        val adbToken = LocalAdbToken.current

        PScaffold(
            topBar = {
                PTopAppBar(
                    navController = navController,
                    title = stringResource(Res.string.developer_options),
                )
            },
            content = { paddingValues ->
                LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    item {
                        TopSpace()
                        Subtitle(text = stringResource(Res.string.adb_automation))
                        ClipboardCard(label = stringResource(Res.string.token), text = adbToken)
                        VerticalSpace(dp = 16.dp)
                        ClipboardCard(
                            label = stringResource(Res.string.adb_cmd_start),
                            text = "adb shell am broadcast -a $packageId.action.START_HTTP_SERVER -p $packageId --es token $adbToken",
                        )
                        VerticalSpace(dp = 16.dp)
                        ClipboardCard(
                            label = stringResource(Res.string.adb_cmd_stop),
                            text = "adb shell am broadcast -a $packageId.action.STOP_HTTP_SERVER -p $packageId --es token $adbToken",
                        )
                        Tips(text = stringResource(Res.string.adb_token_desc))
                        VerticalSpace(dp = 16.dp)
                        PFilledButton(
                            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                            text = stringResource(Res.string.reset_token),
                            type = ButtonType.DANGER,
                            onClick = onResetToken,
                        )
                        BottomSpace(paddingValues)
                    }
                }
            },
        )
    }
}
