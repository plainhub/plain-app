package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.preference.AuthDevTokenPreference
import com.ismartcoding.plain.preference.LocalAuthDevToken
import com.ismartcoding.plain.preference.WebSettingsProvider
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.ClipboardCard
import com.ismartcoding.plain.ui.base.PBlockButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDevPage(navController: NavHostController) {
    WebSettingsProvider {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val devToken = LocalAuthDevToken.current
        var enable by remember { mutableStateOf(false) }
        val httpPort = TempData.httpPort
        val ip4 = remember { NetworkHelper.getDeviceIP4().ifEmpty { "127.0.0.1" } }

        LaunchedEffect(devToken) {
            enable = devToken.isNotEmpty()
        }

        PScaffold(
            topBar = {
                PTopAppBar(navController = navController, title = stringResource(R.string.testing_token))
            },
            content = { paddingValues ->
                LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    item {
                        TopSpace()
                        PCard {
                            PListItem(
                                title = stringResource(R.string.enable_testing_token),
                            ) {
                                PSwitch(
                                    activated = enable,
                                ) {
                                    scope.launch(Dispatchers.IO) {
                                        AuthDevTokenPreference.putAsync(context, if (it) CryptoHelper.randomPassword(128) else "")
                                    }
                                }
                            }
                        }
                        if (enable) {
                            VerticalSpace(dp = 16.dp)
                            Subtitle(text = stringResource(id = R.string.token))
                            ClipboardCard(label = stringResource(id = R.string.token), devToken)
                            VerticalSpace(dp = 16.dp)
                            Subtitle(text = "CURL")
                            ClipboardCard(
                                label = "CURL",
                                text = """curl --request POST --url http://$ip4:$httpPort/graphql --header 'Authorization: Bearer $devToken' --header 'Content-Type: application/json' --data '{"query":"{ chatItems { content } }"}'""",
                            )
                            Tips(text = stringResource(id = R.string.auth_dev_token_tips))
                            VerticalSpace(dp = 24.dp)
                            PBlockButton(
                                text = stringResource(id = R.string.reset_token),
                                type = ButtonType.DANGER,
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        AuthDevTokenPreference.putAsync(context, CryptoHelper.randomPassword(128))
                                    }
                                },
                            )
                        }
                        BottomSpace()
                    }
                }
            },
        )
    }
}
