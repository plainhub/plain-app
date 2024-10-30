package com.ismartcoding.plain.ui.page.root.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PMainSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.WebAddress
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.nav.Routing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeWeb(
    context: Context,
    navController: NavHostController,
    mainVM: MainViewModel,
    webEnabled: Boolean,
) {
    PCard {
        PListItem(
            modifier = Modifier.clickable {
                navController.navigate(Routing.WebSettings)
            },
            title = stringResource(R.string.web_console),
            showMore = true,
        )
        VerticalSpace(dp = 8.dp)
        PMainSwitch(
            title = stringResource(id = mainVM.httpServerState.getTextId()),
            activated = webEnabled,
            enable = !mainVM.httpServerState.isProcessing()
        ) { it ->
            mainVM.enableHttpServer(context, it)
        }
        if (webEnabled) {
            WebAddress(context)
        }
        VerticalSpace(dp = 16.dp)
    }
}
