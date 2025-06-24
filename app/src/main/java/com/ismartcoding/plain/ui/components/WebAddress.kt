package com.ismartcoding.plain.ui.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.preferences.HttpsPreference
import com.ismartcoding.plain.ui.base.PageIndicator
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.MainViewModel
import kotlinx.coroutines.launch

// https://developer.android.com/jetpack/compose/layouts/pager
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WebAddress(
    context: Context,
    mainVM: MainViewModel
) {
    val initialPage = if (TempData.webHttps) 1 else 0
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = {
        2
    })
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState) {
        if (initialPage != pagerState.currentPage) {
            scope.launch {
                pagerState.animateScrollToPage(initialPage)
            }
        }
        snapshotFlow { pagerState.currentPage }.collect { page ->
            HttpsPreference.putAsync(context, page == 1)
        }
    }

    VerticalSpace(dp = 16.dp)
    HorizontalPager(
        modifier = Modifier.padding(horizontal = 16.dp),
        state = pagerState,
        pageSpacing = 16.dp,
    ) { page ->
        Column {
            val isHttps = page != 0
            WebAddressBar(context, mainVM, isHttps)
            Tips(text = stringResource(id = R.string.enter_this_address_tips), modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp))
            VerticalSpace(dp = 8.dp)
        }
    }
    PageIndicator(pagerState)
}