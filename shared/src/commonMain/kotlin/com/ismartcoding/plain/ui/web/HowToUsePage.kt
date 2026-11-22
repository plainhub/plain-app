package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.web.WebHelper
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.StepItem
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.settings.DesktopAccessSettingsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val TROUBLESHOOTING_GUIDE_URL = "https://plainapp.app/docs/troubleshooting"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUsePage(navController: NavHostController, webVM: DesktopAccessSettingsViewModel) {
    val addresses by webVM.lanIpv4Addresses.collectAsState()
    val demoIp = addresses?.firstOrNull() ?: "192.0.2.20"
    LaunchedEffect(webVM) { webVM.observeNetworkAddresses() }

    PScaffold(topBar = {
        PTopAppBar(onNavigateBack = { navController.navigateUp() }, title = stringResource(Res.string.how_to_use))
    }) { insets ->
        Column(Modifier.padding(insets).verticalScroll(rememberScrollState())
            .padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN, vertical = 16.dp)) {
            PSectionTitle(stringResource(Res.string.guide_connect))
            PCard {
                StepItem(1, stringResource(Res.string.step_connect_network_title), stringResource(Res.string.step_connect_network_desc))
                StepItem(2, stringResource(Res.string.guide_enable), stringResource(Res.string.guide_enable_desc))
                StepItem(3, stringResource(Res.string.step_open_url_title), stringResource(Res.string.guide_address_desc))
            }
            VerticalSpace(16.dp)
            PSectionTitle(stringResource(Res.string.troubleshooting))
            PCard {
                GuideQuestion(Res.string.faq_cannot_open_q, Res.string.guide_unreachable) {
                    PTextButton(stringResource(Res.string.run_diagnostics), onClick = webVM::checkHttpServerHealth)
                }
                GuideQuestion(Res.string.faq_https_warning_q, Res.string.guide_certificate) {
                    VerticalSpace(8.dp)
                    FaqCertDemo(serverIp = demoIp)
                }
                GuideQuestion(Res.string.faq_stay_online_q, Res.string.faq_stay_online_a) {
                    VerticalSpace(8.dp)
                    FaqStayOnlineDemo()
                }
                GuideQuestion(Res.string.guide_missing_feature, Res.string.guide_permissions)
            }
            VerticalSpace(8.dp)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PTextButton(stringResource(Res.string.onboarding_replay), onClick = { navController.navigate(Routing.Onboarding) })
                PTextButton(stringResource(Res.string.more_help), onClick = { WebHelper.open(TROUBLESHOOTING_GUIDE_URL) })
            }
        }
    }
}

@Composable
private fun GuideQuestion(question: StringResource, answer: StringResource, action: (@Composable () -> Unit)? = null) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    PExpandableSection(text = stringResource(question), expanded = expanded,
        stateDescription = stringResource(if (expanded) Res.string.expanded else Res.string.collapsed),
        onClick = { expanded = !expanded }) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Text(stringResource(answer), style = MaterialTheme.typography.bodyMedium)
            action?.invoke()
        }
    }
}
