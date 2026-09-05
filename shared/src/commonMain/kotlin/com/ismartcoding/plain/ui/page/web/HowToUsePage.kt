package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.preferences.HttpsPreference
import com.ismartcoding.plain.preferences.WebSettingsProvider
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.FaqItem
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.StepItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.WebAddressBar
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.blue
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.theme.grey
import com.ismartcoding.plain.ui.theme.orange
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val TROUBLESHOOTING_GUIDE_URL = "https://plainapp.app/docs/troubleshooting"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUsePage(
    navController: NavHostController,
    onRunDiagnostics: () -> Unit,
) {
    WebSettingsProvider {
        val serviceEnabled = TempData.serviceEnabled.collectAsStateValue()
        val isHttps = TempData.webHttps.collectAsState()
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(
            initialPage = if (isHttps.value) 1 else 0,
            pageCount = { 2 },
        )

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                val https = page == 1
                if (isHttps.value != https) {
                    scope.launch { HttpsPreference.putAsync(https) }
                }
            }
        }

        PScaffold(
            topBar = {
                PTopAppBar(navController = navController, title = stringResource(Res.string.how_to_use))
            },
            content = { paddingValues ->
                LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    item {
                        TopSpace()
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (serviceEnabled) MaterialTheme.colorScheme.green else MaterialTheme.colorScheme.grey),
                                )
                                HorizontalSpace(dp = 8.dp)
                                Text(
                                    text = stringResource(if (serviceEnabled) Res.string.http_server_state_on else Res.string.service_not_running),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth(),
                            ) { page ->
                                WebAddressBar(isHttps = page == 1)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                repeat(2) { index ->
                                    val selected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            ),
                                    )
                                }
                            }
                            VerticalSpace(dp = 12.dp)
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                    item {
                        Subtitle(text = stringResource(Res.string.quick_start))
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                StepItem(index = 1, title = stringResource(Res.string.step_connect_network_title), desc = stringResource(Res.string.step_connect_network_desc))
                                StepItem(index = 2, title = stringResource(Res.string.step_open_url_title), desc = stringResource(Res.string.enter_this_address_tips))
                            }
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                    item {
                        Subtitle(text = stringResource(Res.string.troubleshooting))
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            FaqItem(
                                icon = Res.drawable.circle_alert,
                                tint = MaterialTheme.colorScheme.red,
                                question = stringResource(Res.string.faq_cannot_open_q),
                                answer = stringResource(Res.string.faq_cannot_open_a),
                            ) {
                                RunDiagnosticsChip(onClick = onRunDiagnostics)
                            }
                            FaqItem(
                                icon = Res.drawable.lock,
                                tint = MaterialTheme.colorScheme.orange,
                                question = stringResource(Res.string.faq_https_warning_q),
                                answer = stringResource(Res.string.browser_https_error_tips),
                            )
                            FaqItem(
                                icon = Res.drawable.timer,
                                tint = MaterialTheme.colorScheme.blue,
                                question = stringResource(Res.string.faq_port_conflict_q),
                                answer = stringResource(Res.string.faq_port_conflict_a),
                            )
                            FaqItem(
                                icon = Res.drawable.wifi,
                                tint = MaterialTheme.colorScheme.green,
                                question = stringResource(Res.string.faq_slow_q),
                                answer = stringResource(Res.string.faq_slow_a),
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            PListItem(
                                modifier = Modifier.clickable { WebHelper.open(TROUBLESHOOTING_GUIDE_URL) },
                                icon = Res.drawable.circle_help,
                                title = stringResource(Res.string.more_help),
                                subtitle = TROUBLESHOOTING_GUIDE_URL.removePrefix("https://"),
                                showMore = true,
                            )
                        }
                        BottomSpace(paddingValues)
                    }
                }
            },
        )
    }
}

@Composable
private fun RunDiagnosticsChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(Res.drawable.troubleshoot),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            HorizontalSpace(dp = 4.dp)
            Text(
                text = stringResource(Res.string.run_diagnostics),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
