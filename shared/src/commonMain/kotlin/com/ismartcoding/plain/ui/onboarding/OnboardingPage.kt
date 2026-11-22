package com.ismartcoding.plain.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import kotlinx.coroutines.launch
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
            PTextButton(
                text = stringResource(Res.string.onboarding_skip),
                onClick = { complete(false) },
                modifier = Modifier.align(Alignment.End),
            )
            HorizontalPager(pages, Modifier.weight(1f)) { page ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    when (page) {
                        0 -> DirectConnectionIllustration()
                        1 -> RemoteAccessAnimation(active = pages.settledPage == page)
                        else -> AccessSettingsIllustration()
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
            PRangeDots(
                items = (0 until pages.pageCount).toList(),
                selected = pages.currentPage,
                onSelect = { page -> scope.launch { pages.animateScrollToPage(page) } },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            val last = pages.currentPage == pages.pageCount - 1
            PFilledButton(text = stringResource(if (last) Res.string.wizard_view_guide else Res.string.onboarding_next),
                onClick = {
                    if (last) complete(true)
                    else scope.launch { pages.animateScrollToPage(pages.currentPage + 1) }
                }, buttonSize = ButtonSize.LARGE, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        }
    }
}
