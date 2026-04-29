package com.ismartcoding.plain.ui.page.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.preferences.HttpsPreference
import com.ismartcoding.plain.ui.base.PIconTextButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.WebAddressBar
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.launch

@Composable
fun HomeWebAddressSection(
    context: Context,
    navController: NavHostController,
    mainVM: MainViewModel,
    isError: Boolean
) {
    var isHttps by remember { mutableStateOf(TempData.webHttps) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = if (isHttps) 1 else 0,
        pageCount = { 2 },
    )
    var showStayOnlineOverlay by remember { mutableStateOf(false) }

    if (showStayOnlineOverlay) {
        StayOnlineModeOverlay(onExit = { showStayOnlineOverlay = false })
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val https = page == 1
            if (isHttps != https) {
                isHttps = https
                scope.launch { HttpsPreference.putAsync(context, https) }
            }
        }
    }

    Column {
        Text(
            text = stringResource(R.string.web_address_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        VerticalSpace(12.dp)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                WebAddressBar(context = context, mainVM = mainVM, isHttps = page == 1)
            }
            VerticalSpace(8.dp)
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
            VerticalSpace(12.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (isError) {
                    POutlinedButton(
                        stringResource(R.string.troubleshoot),
                        onClick = {
                            WebHelper.open(
                                context,
                                "https://plainapp.app/troubleshooting"
                            )
                        },
                    )
                } else {
                    PIconTextButton(
                        icon = R.drawable.wifi_tethering,
                        text = stringResource(R.string.stay_online_mode),
                        click = { showStayOnlineOverlay = true },
                    )
                }
                PIconTextButton(R.drawable.settings, stringResource(R.string.web_settings)) {
                    navController.navigate(Routing.WebSettings)
                }
            }
        }
        VerticalSpace(8.dp)
        Tips(text = stringResource(R.string.same_network_hint))
    }
}
