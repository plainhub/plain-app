package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.preference.AuthDevTokenPreference
import com.ismartcoding.plain.data.preference.LocalAuthDevToken
import com.ismartcoding.plain.data.preference.WebSettingsProvider
import com.ismartcoding.plain.ui.base.*

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

        LaunchedEffect(devToken) {
            enable = devToken.isNotEmpty()
        }

        PScaffold(
            navController,
            content = {
                LazyColumn {
                    item {
                        DisplayText(text = stringResource(R.string.testing_token))
                        PListItem(
                            title = stringResource(R.string.enable_testing_token),
                        ) {
                            PSwitch(
                                activated = enable
                            ) {
                                AuthDevTokenPreference.put(context, if (it) CryptoHelper.randomPassword(128) else "")
                            }
                        }
                        if (enable) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Subtitle(text = stringResource(id = R.string.token))
                            PListItem(
                                title = devToken,
                            ) {
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            BlockOutlineButton(
                                text = stringResource(id = R.string.reset_token),
                                onClick = {
                                    AuthDevTokenPreference.put(context, CryptoHelper.randomPassword(128))
                                })
                            Spacer(modifier = Modifier.height(16.dp))
                            Tips(text = stringResource(id = R.string.auth_dev_token_tips))
                        }
                        BottomSpace()
                    }
                }
            }
        )
    }
}
