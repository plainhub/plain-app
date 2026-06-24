package com.ismartcoding.plain.ui.page.connections

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.ClipboardCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.WebHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTokenTipsPage(
    navController: NavHostController,
    clientId: String,
    token: String,
) {
    val context = LocalContext.current
    val hostname = remember { TempData.mdnsHostname }
    val httpPort = TempData.httpPort.collectAsState()

    val curlReal = remember(clientId, token, hostname, httpPort.value) {
        """curl -X POST "http://$hostname:${httpPort.value}/graphql" -H "c-id: $clientId" -H "Authorization: Bearer $token" -H "Content-Type: application/json" --data '{"query":"{ app { appVersion } }"}'"""
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.api_tokens) + " · " + stringResource(Res.string.how_to_use),
            )
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item { TopSpace() }
                item {
                    ClipboardCard(
                        label = "CURL",
                        text = curlReal,
                    )
                    Tips(
                        text = stringResource(Res.string.auth_dev_token_tips),
                    )
                    VerticalSpace(dp = 16.dp)
                    PFilledButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        text = stringResource(Res.string.docs),
                        onClick = { WebHelper.open(context, "https://plainapp.app/api-docs") },
                    )
                    BottomSpace(paddingValues)
                }
            }
        },
    )
}
