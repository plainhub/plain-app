package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.ismartcoding.plain.data.enums.ButtonType
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.data.preference.LocalPassword
import com.ismartcoding.plain.data.preference.LocalPasswordType
import com.ismartcoding.plain.data.preference.UrlTokenPreference
import com.ismartcoding.plain.data.preference.WebSettingsProvider
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.ClipboardCard
import com.ismartcoding.plain.ui.base.PBlockButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSecurityPage(navController: NavHostController) {
    WebSettingsProvider {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val passwordType = LocalPasswordType.current
        val password = LocalPassword.current
        var urlToken by remember { mutableStateOf(TempData.urlToken) }

        PScaffold(
            navController,
            topBarTitle = stringResource(R.string.security),
            content = {
                LazyColumn {
                    item {
                        TopSpace()
                        PCard {
                            PListItem(
                                title = stringResource(R.string.password_settings),
                                value = if (passwordType == PasswordType.NONE.value) stringResource(id = R.string.password_type_none) else password,
                                onClick = {
                                    navController.navigate(RouteName.PASSWORD)
                                },
                                showMore = true,
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                    item {
                        Subtitle(text = stringResource(id = R.string.https_certificate_signature))
                        ClipboardCard(
                            label =
                            stringResource(
                                id = R.string.https_certificate_signature,
                            ),
                            HttpServerManager.getSSLSignature(context).joinToString(" ") {
                                "%02x".format(it).uppercase()
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Subtitle(text = stringResource(id = R.string.url_token))
                        ClipboardCard(label = stringResource(id = R.string.url_token), urlToken)
                        Tips(text = stringResource(id = R.string.url_token_tips))
                        Spacer(modifier = Modifier.height(16.dp))
                        PBlockButton(
                            text = stringResource(id = R.string.reset_token),
                            type = ButtonType.DANGER,
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    UrlTokenPreference.resetAsync(context)
                                    urlToken = TempData.urlToken
                                }
                            },
                        )
                        BottomSpace()
                    }
                }
            },
        )
    }
}
