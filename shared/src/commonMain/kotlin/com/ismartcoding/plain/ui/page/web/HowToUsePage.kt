package com.ismartcoding.plain.ui.page.web
import com.ismartcoding.plain.ui.theme.PlainTheme

import androidx.compose.foundation.layout.fillMaxWidth

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
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.preferences.WebSettingsProvider
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.collectAsStateValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUsePage(
    navController: NavHostController,
    onRunDiagnostics: () -> Unit,
) {
    WebSettingsProvider {
        val serviceEnabled = TempData.serviceEnabled.collectAsStateValue()
        PScaffold(
            topBar = {
                PTopAppBar(navController = navController, title = stringResource(Res.string.how_to_use))
            },
            content = { paddingValues ->
                LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    item {
                        TopSpace()
                        Subtitle(text = stringResource(Res.string.instruction_for_use))
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
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
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            Text(
                                stringResource(Res.string.usb_connect_recommendation),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                    item {
                        if (serviceEnabled) {
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
