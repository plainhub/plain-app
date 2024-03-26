package com.ismartcoding.plain.ui.components.home

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PClickableText
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PMainSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.WebAddress
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.VClickText
import com.ismartcoding.plain.ui.page.RouteName

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeWeb(
    context: Context,
    navController: NavHostController,
    viewModel: MainViewModel,
    webEnabled: Boolean,
) {
    val learnMore = stringResource(id = R.string.learn_more)
    val fullText = (stringResource(id = R.string.web_console_desc) + " " + learnMore)
    PCard {
        PListItem(
            title = stringResource(R.string.web_console),
            showMore = true,
            onClick = {
                navController.navigate(RouteName.WEB_SETTINGS)
            }
        )
        VerticalSpace(dp = 8.dp)
        PClickableText(
            text = fullText,
            clickTexts = listOf(
                VClickText(learnMore) {
                    navController.navigate(RouteName.WEB_LEARN_MORE)
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
        VerticalSpace(dp = 8.dp)
        PMainSwitch(
            title = stringResource(id = viewModel.httpServerState.getTextId()),
            activated = webEnabled,
            enable = !viewModel.httpServerState.isProcessing()
        ) { it ->
            viewModel.enableHttpServer(context, it)
        }
        if (webEnabled) {
            WebAddress(context)
        }
        VerticalSpace(dp = 16.dp)
    }
}
