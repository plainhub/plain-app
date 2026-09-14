package com.ismartcoding.plain.ui.page.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.preferences.OnboardingPreference
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.StepNumber
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val PAGE_COUNT = 5

@Composable
fun OnboardingPage(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })

    fun complete(navigateToHowToUse: Boolean) {
        scope.launch {
            OnboardingPreference.putAsync(true)
            if (navigateToHowToUse) {
                navController.navigate(Routing.HowToUse) {
                    popUpTo(Routing.Onboarding) { inclusive = true }
                }
            } else {
                navController.popBackStack()
            }
        }
    }

    PBackHandler { complete(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = stringResource(Res.string.onboarding_skip),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { complete(false) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> SlideWelcome()
                1 -> SlideDesktopAccess()
                2 -> SlideChat()
                3 -> SlideMediaTools()
                else -> SlideGetStarted()
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(PAGE_COUNT) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(if (selected) 20.dp else 8.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                )
            }
        }
        val isLast = pagerState.currentPage == PAGE_COUNT - 1
        PFilledButton(
            text = stringResource(if (isLast) Res.string.onboarding_get_started else Res.string.onboarding_next),
            onClick = {
                if (isLast) {
                    complete(true)
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )
        VerticalSpace(dp = 28.dp)
    }
}

@Composable
private fun SlideWelcome() {
    SlideFrame(
        illustration = { WelcomeIllustration() },
        title = stringResource(Res.string.onboarding_1_title),
        body = stringResource(Res.string.onboarding_1_body),
        badges = listOf(
            stringResource(Res.string.onboarding_badge_no_account),
            stringResource(Res.string.onboarding_badge_no_cloud),
            stringResource(Res.string.onboarding_badge_no_ads),
        ),
    )
}

@Composable
private fun SlideDesktopAccess() {
    SlideFrame(
        illustration = { DesktopAccessIllustration() },
        title = stringResource(Res.string.onboarding_2_title),
        body = stringResource(Res.string.onboarding_2_body),
    )
}

@Composable
private fun SlideChat() {
    SlideFrame(
        illustration = { ChatIllustration() },
        title = stringResource(Res.string.onboarding_3_title),
        body = stringResource(Res.string.onboarding_3_body),
    )
}

@Composable
private fun SlideMediaTools() {
    SlideFrame(
        illustration = { MediaToolsIllustration() },
        title = stringResource(Res.string.onboarding_4_title),
        body = stringResource(Res.string.onboarding_4_body),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlideFrame(
    title: String,
    body: String,
    illustration: @Composable () -> Unit,
    badges: List<String> = emptyList(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center,
        ) {
            illustration()
        }
        VerticalSpace(dp = 8.dp)
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        VerticalSpace(dp = 12.dp)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (badges.isNotEmpty()) {
            VerticalSpace(dp = 16.dp)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                badges.forEach { BadgeChip(it) }
            }
        }
    }
}

@Composable
private fun BadgeChip(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.check),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SlideGetStarted() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.onboarding_5_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        VerticalSpace(dp = 28.dp)
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            StepRow(1, stringResource(Res.string.onboarding_step1_title), stringResource(Res.string.onboarding_step1_desc))
            StepRow(2, stringResource(Res.string.onboarding_step2_title), stringResource(Res.string.onboarding_step2_desc))
            StepRow(3, stringResource(Res.string.onboarding_step3_title), stringResource(Res.string.onboarding_step3_desc))
        }
        VerticalSpace(dp = 24.dp)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.cardBackgroundNormal)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.lock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "https://192.168.1.20",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            )
        }
        VerticalSpace(dp = 16.dp)
        Text(
            text = stringResource(Res.string.onboarding_revisit_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StepRow(index: Int, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        StepNumber(num = index)
        Column {
            VerticalSpace(dp = 4.dp)
            Text(text = title, style = MaterialTheme.typography.listItemTitle())
            VerticalSpace(dp = 8.dp)
            Text(text = description, style = MaterialTheme.typography.listItemSubtitle())
            VerticalSpace(dp = 8.dp)
        }
    }
}
