package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.GridItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.page.RouteName

@Composable
fun HomeItemTools(
    navController: NavHostController,
) {
    Column {
        Subtitle(
            text = stringResource(R.string.home_item_title_tools)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GridItem(
                icon = Icons.Outlined.CurrencyExchange,
                stringResource(id = R.string.exchange_rate),
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate(RouteName.EXCHANGE_RATE)
            }
            Spacer(modifier = Modifier.width(8.dp))
            GridItem(
                icon = Icons.Outlined.GraphicEq,
                stringResource(id = R.string.sound_meter),
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate(RouteName.SOUND_METER)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Spacer(modifier = Modifier.weight(2f))
        }
    }
}