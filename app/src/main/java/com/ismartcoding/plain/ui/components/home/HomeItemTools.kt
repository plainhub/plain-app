package com.ismartcoding.plain.ui.components.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.GridItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.page.RouteName

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeItemTools(
    navController: NavHostController,
    itemWidth: Dp,
) {
    Column {
        Subtitle(
            text = stringResource(R.string.home_item_title_tools),
        )
        HomeItemFlow {
            GridItem(
                icon = Icons.Outlined.CurrencyExchange,
                stringResource(id = R.string.exchange_rate),
                modifier = Modifier.width(itemWidth),
            ) {
                navController.navigate(RouteName.EXCHANGE_RATE)
            }
            GridItem(
                icon = Icons.Outlined.GraphicEq,
                stringResource(id = R.string.sound_meter),
                modifier = Modifier.width(itemWidth),
            ) {
                navController.navigate(RouteName.SOUND_METER)
            }
        }
    }
}
