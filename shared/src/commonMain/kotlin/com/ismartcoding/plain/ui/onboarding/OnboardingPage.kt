package com.ismartcoding.plain.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.devices
import com.ismartcoding.plain.ui.resources.lock
import com.ismartcoding.plain.ui.resources.settings
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingPage(navController: NavHostController) {
    val pages = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()

    fun complete(openGuide: Boolean) {
        scope.launch {
            Prefs.completeOnboarding()
            if (openGuide) {
                navController.navigate(Routing.HowToUse) {
                    popUpTo(Routing.Onboarding) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                navController.popBackStack()
            }
        }
    }

    PBackHandler { complete(false) }
    PScaffold { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(pages.pageCount) { index ->
                        Spacer(Modifier.size(24.dp, 4.dp).background(
                            if (index == pages.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small,
                        ))
                    }
                }
                PTextButton(stringResource(Res.string.onboarding_skip), onClick = { complete(false) })
            }
            HorizontalPager(pages, Modifier.weight(1f)) { page ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.extraLarge)
                        .heightIn(min = 240.dp).padding(24.dp), contentAlignment = Alignment.Center) {
                        when (page) {
                            0 -> Image(painterResource(Res.drawable.onboarding_devices), null, Modifier.fillMaxWidth().height(176.dp))
                            1 -> RemoteAccessPreview()
                            else -> PCard {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(stringResource(Res.string.access_settings), fontWeight = FontWeight.Bold)
                                    listOf(UiRes.drawable.devices to Res.string.connections, UiRes.drawable.lock to Res.string.security,
                                        UiRes.drawable.settings to Res.string.access_remote_permissions).forEach { (icon, label) ->
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(painterResource(icon), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text(stringResource(label))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    VerticalSpace(24.dp)
                    val (title, body) = when (page) {
                        0 -> Res.string.onboarding_1_title to Res.string.onboarding_1_body
                        1 -> Res.string.wizard_connect_title to Res.string.wizard_connect_body
                        else -> Res.string.wizard_access_title to Res.string.wizard_access_body
                    }
                    Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    VerticalSpace(8.dp)
                    Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (page == 0) {
                        VerticalSpace(16.dp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Res.string.onboarding_badge_no_account, Res.string.onboarding_badge_no_ads).forEach {
                                Text(stringResource(it), Modifier.background(MaterialTheme.colorScheme.cardBackgroundNormal,
                                    MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            val last = pages.currentPage == pages.pageCount - 1
            PFilledButton(text = stringResource(if (last) Res.string.wizard_view_guide else Res.string.onboarding_next),
                onClick = {
                    if (last) complete(true)
                    else scope.launch { pages.animateScrollToPage(pages.currentPage + 1) }
                }, buttonSize = ButtonSize.LARGE, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        }
    }
}

@Composable
private fun RemoteAccessPreview() {
    var https by rememberSaveable { mutableStateOf(true) }
    PCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.home_remote_access), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Switch(checked = true, onCheckedChange = null)
            }
            Row {
                listOf(false, true).forEach { secure ->
                    PTextButton(if (secure) "HTTPS" else "HTTP", onClick = { https = secure },
                        contentColor = if (secure == https) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(if (https) "https://192.0.2.20" else "http://192.0.2.20", fontFamily = FontFamily.Monospace)
            Text(stringResource(Res.string.wizard_example_address), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
