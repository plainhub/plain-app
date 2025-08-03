package com.ismartcoding.plain.ui.components

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.preferences.HttpsPreference
import com.ismartcoding.plain.preferences.MdnsHostnamePreference
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PageIndicator
import com.ismartcoding.plain.ui.base.TextFieldDialog
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
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

    var showHostnameDialog by remember { mutableStateOf(false) }
    var hostname by remember { mutableStateOf(TempData.mdnsHostname) }

    VerticalSpace(dp = 8.dp)

    PListItem(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(12.dp),
            ),
        title = "mDNS",
        subtitle = hostname,
        action = {
            PIconButton(icon = R.drawable.pen, contentDescription = stringResource(id = R.string.edit), click = {
                showHostnameDialog = true
            })
        })

    VerticalSpace(dp = 8.dp)

    // HTTP/HTTPS address section
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

    // Hostname edit dialog
    if (showHostnameDialog) {
        TextFieldDialog(
            title = stringResource(id = R.string.mdns_hostname),
            value = hostname,
            confirmText = stringResource(R.string.save),
            placeholder = hostname,
            onDismissRequest = { showHostnameDialog = false },
            onConfirm = { newHostname ->
                hostname = newHostname
                TempData.mdnsHostname = newHostname
                scope.launch {
                    withIO { MdnsHostnamePreference.putAsync(context, newHostname) }
                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle(R.string.restart_app_title)
                        .setMessage(R.string.restart_app_message)
                        .setPositiveButton(R.string.relaunch_app) { _, _ ->
                            AppHelper.relaunch(context)
                        }
                        .setCancelable(false)
                        .create()
                        .show()
                }
                showHostnameDialog = false
            },
            validator = { input ->
                input.isNotEmpty() && input.endsWith(".local")
            },
            validationErrorText = stringResource(id = R.string.mdns_hostname_invalid)
        )
    }
}