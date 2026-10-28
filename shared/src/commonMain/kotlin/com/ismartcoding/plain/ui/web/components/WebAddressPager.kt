package com.ismartcoding.plain.ui.web.components

import com.ismartcoding.plain.preferences.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.base.PSegmentedButtons
import com.ismartcoding.plain.ui.base.PRangeDots
import com.ismartcoding.plain.ui.base.PSegmentedButtonsDefaults
import com.ismartcoding.plain.ui.components.ButtonSize
import kotlinx.coroutines.launch

@Composable
fun WebAddressPager(
    lanIpv4Addresses: List<String>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val isHttps = Prefs.https.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = if (isHttps.value) 1 else 0,
        pageCount = { 2 },
    )
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            Prefs.set(PrefKeys.HTTPS, page == 1)
        }
    }
    Column(
        modifier = modifier.fillMaxWidth().then(
            if (compact) Modifier.background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large).padding(8.dp)
            else Modifier.padding(horizontal = 8.dp),
        ),
    ) {
        if (compact) {
            PSegmentedButtons(
                options = listOf("HTTP", "HTTPS"),
                selectedIndex = pagerState.currentPage,
                onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                label = { it },
                modifier = Modifier.width(IntrinsicSize.Max),
                buttonSize = ButtonSize.LARGE,
                content = { option, _ -> Text(option, style = MaterialTheme.typography.labelMedium) },
                colors = PSegmentedButtonsDefaults.colors(
                    containerColor = Color.Transparent,
                    selectedContainerColor = MaterialTheme.colorScheme.surface,
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            WebAddressBar(isHttps = page == 1, lanIpv4Addresses = lanIpv4Addresses, compact = compact)
        }
        if (!compact) {
            PRangeDots(
                items = listOf(0, 1),
                selected = pagerState.currentPage,
                onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
