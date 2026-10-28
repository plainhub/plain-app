package com.ismartcoding.plain.ui.web.components

import com.ismartcoding.plain.preferences.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.Prefs
import kotlinx.coroutines.launch

@Composable
fun WebAddressPager(
    lanIpv4Addresses: List<String>,
    modifier: Modifier = Modifier,
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
        modifier = modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(top = 8.dp),
    ) {
        val labelStyle = MaterialTheme.typography.labelLarge
        val textMeasurer = rememberTextMeasurer()
        val optionWidth = with(LocalDensity.current) {
            textMeasurer.measure("HTTPS", labelStyle.copy(fontWeight = FontWeight.Bold)).size.width.toDp() + 16.dp
        }.coerceAtLeast(48.dp)
        Row(Modifier.selectableGroup().padding(start = 16.dp)) {
            listOf("HTTP", "HTTPS").forEachIndexed { page, label ->
                val selected = pagerState.currentPage == page
                Box(
                    modifier = Modifier.width(optionWidth).heightIn(min = 48.dp).selectable(
                        selected = selected,
                        role = Role.Tab,
                        interactionSource = null,
                        indication = null,
                        onClick = { scope.launch { pagerState.animateScrollToPage(page) } },
                    ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = label,
                        style = labelStyle,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            WebAddressBar(isHttps = page == 1, lanIpv4Addresses = lanIpv4Addresses)
        }
    }
}
