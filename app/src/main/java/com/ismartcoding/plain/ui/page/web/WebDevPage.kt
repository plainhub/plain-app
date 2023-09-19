package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.ismartcoding.plain.data.preference.AuthDevTokenPreference
import com.ismartcoding.plain.data.preference.LocalAuthDevToken
import com.ismartcoding.plain.data.preference.LocalHttpPort
import com.ismartcoding.plain.data.preference.WebSettingsProvider
import com.ismartcoding.plain.ui.base.*
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
        val devToken = LocalAuthDevToken.current
        var enable by remember { mutableStateOf(false) }
        val httpPort = LocalHttpPort.current
        val ip4 = remember { NetworkHelper.getDeviceIP4().ifEmpty { "127.0.0.1" } }

        LaunchedEffect(devToken) {
            enable = devToken.isNotEmpty()
        }

        PScaffold(
            navController,
            topBarTitle = stringResource(R.string.testing_token),
            content = {
                LazyColumn {
                    item {
                        VerticalSpace(dp = 16.dp)
                    }
                    item {
                        PListItem(
                            title = stringResource(R.string.enable_testing_token),
                        ) {
                            PSwitch(
                                activated = enable
                            ) {
                                scope.launch(Dispatchers.IO) {
                                    AuthDevTokenPreference.putAsync(context, if (it) CryptoHelper.randomPassword(128) else "")
                                }
                            }
                        }
                        if (enable) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Subtitle(text = stringResource(id = R.string.token), Modifier.padding(horizontal = 32.dp))
                            ClipboardCard(label = stringResource(id = R.string.token), devToken)
                            Spacer(modifier = Modifier.height(24.dp))
                            BlockOutlineButton(
                                text = stringResource(id = R.string.reset_token),
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        AuthDevTokenPreference.putAsync(context, CryptoHelper.randomPassword(128))
                                    }
                                })
                            Spacer(modifier = Modifier.height(16.dp))
                            Tips(text = stringResource(id = R.string.auth_dev_token_tips))
                            Subtitle(text = "CURL", Modifier.padding(horizontal = 32.dp))
                            ClipboardCard(
                                label = "CURL",
                                text = """curl --request POST --url http://${ip4}:${httpPort}/graphql --header 'Authorization: Bearer ${devToken}' --header 'Content-Type: application/json' --data '{"query":"{ chatItems { content } }"}'"""
                            )
                        }
                        BottomSpace()
                    }
                }
            }
        )
    }
}
