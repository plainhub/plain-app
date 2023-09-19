package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.preference.UrlTokenPreference
import com.ismartcoding.plain.data.preference.WebSettingsProvider
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSecurityPage(
    navController: NavHostController,
) {
    WebSettingsProvider {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var urlToken by remember { mutableStateOf(TempData.urlToken) }

        PScaffold(
            navController,
            topBarTitle = stringResource(R.string.security),
            content = {
                LazyColumn {
                    item {
                        VerticalSpace(dp = 16.dp)
                    }
                    item {
                        Subtitle(text = stringResource(id = R.string.https_certificate_signature), Modifier.padding(horizontal = 32.dp))
                        ClipboardCard(label = stringResource(id = R.string.https_certificate_signature), HttpServerManager.getSSLSignature(context).joinToString(" ") { "%02x".format(it).uppercase() })
                        Spacer(modifier = Modifier.height(16.dp))
                        Subtitle(text = stringResource(id = R.string.url_token), Modifier.padding(horizontal = 32.dp))
                        ClipboardCard(label = stringResource(id = R.string.url_token), urlToken)
                        Tips(text = stringResource(id = R.string.url_token_tips))
                        Spacer(modifier = Modifier.height(16.dp))
                        BlockOutlineButton(
                            text = stringResource(id = R.string.reset_token),
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    UrlTokenPreference.resetAsync(context)
                                    urlToken = TempData.urlToken
                                }
                            })
                        BottomSpace()
                    }
                }
            }
        )
    }
}
