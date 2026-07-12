package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.layout.fillMaxWidth
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.preferences.LocalWeb
import com.ismartcoding.plain.preferences.WebSettingsProvider
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUsePage(
    navController: NavHostController,
    onRunDiagnostics: () -> Unit,
) {

    WebSettingsProvider {
        val webEnabled = LocalWeb.current
        PScaffold(
            topBar = {
                PTopAppBar(navController = navController, title = stringResource(Res.string.how_to_use))
            },
            content = { paddingValues ->
                LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    item {
                        TopSpace()
                        Subtitle(text = stringResource(Res.string.instruction_for_use))
                        PCard {
                            Text(
                                stringResource(Res.string.web_how_to),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                    item {
                        Subtitle(text = stringResource(Res.string.recommendation))
                        PCard {
                            Text(
                                stringResource(Res.string.usb_connect_recommendation),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                    item {
                        if (webEnabled) {
                            VerticalSpace(dp = 16.dp)
                            PFilledButton(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth(),
                                text = stringResource(Res.string.http_server_diagnostics),
                                onClick = onRunDiagnostics,
                            )
                        }
                        BottomSpace(paddingValues)
                    }
                }
            },
        )
    }
}
