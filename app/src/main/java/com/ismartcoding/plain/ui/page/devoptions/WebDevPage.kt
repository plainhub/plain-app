package com.ismartcoding.plain.ui.page.devoptions

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.preferences.AdbTokenPreference
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
import com.ismartcoding.plain.ui.components.showRestartAppDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDevPage(
    navController: NavHostController,
) {
    WebSettingsProvider {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val adbToken = LocalAdbToken.current
        val packageId = BuildConfig.APPLICATION_ID

        PScaffold(
            topBar = {
                PTopAppBar(
                    navController = navController,
                    title = stringResource(R.string.developer_options),
                )
            },
            content = { paddingValues ->
                LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    item {
                        TopSpace()
                        Subtitle(text = stringResource(R.string.adb_automation))
                        ClipboardCard(label = stringResource(R.string.token), text = adbToken)
                        VerticalSpace(dp = 16.dp)
                        ClipboardCard(
                            label = stringResource(R.string.adb_cmd_start),
                            text = "adb shell am broadcast -a $packageId.action.START_HTTP_SERVER -p $packageId --es token $adbToken",
                        )
                        VerticalSpace(dp = 16.dp)
                        ClipboardCard(
                            label = stringResource(R.string.adb_cmd_stop),
                            text = "adb shell am broadcast -a $packageId.action.STOP_HTTP_SERVER -p $packageId --es token $adbToken",
                        )
                        Tips(text = stringResource(R.string.adb_token_desc))
                        VerticalSpace(dp = 16.dp)
                        PFilledButton(
                            text = stringResource(R.string.reset_token),
                            type = ButtonType.DANGER,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    AdbTokenPreference.resetAsync(context)
                                }
                            },
                        )
                        BottomSpace(paddingValues)
                    }
                }
            },
        )
    }
}
