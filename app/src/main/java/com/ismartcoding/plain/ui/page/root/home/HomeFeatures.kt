package com.ismartcoding.plain.ui.page.root.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.preference.HomeFeaturesPreference
import com.ismartcoding.plain.preference.dataFlow
import com.ismartcoding.plain.preference.dataStore
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconTextButton
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeFeatures(
    navController: NavHostController,
    itemWidth: Dp,
) {
    val context = LocalContext.current

    val selectedFeatures = remember {
        context.dataStore.dataFlow.map {
            HomeFeaturesPreference.get(it)
        }
    }.collectAsStateValue(initial = HomeFeaturesPreference.default)

    var showSelectionPage by remember { mutableStateOf(false) }

    if (showSelectionPage) {
        HomeFeaturesSelectionPage(navController, onDismissRequest = { showSelectionPage = false })
    }

    PCard {
        HomeItemFlow {
            FeatureItem.getList(navController).filter { selectedFeatures.contains(it.type.name) }.forEach { item ->
                PIconTextButton(
                    icon = painterResource(item.iconRes),
                    stringResource(id = item.titleRes),
                    modifier = Modifier
                        .width(itemWidth)
                        .clickable {
                            item.click()
                        })
            }

            PIconTextButton(
                icon = painterResource(R.drawable.plus),
                stringResource(id = R.string.more),
                modifier = Modifier
                    .width(itemWidth)
                    .clickable {
                        showSelectionPage = true
                    },
            )
        }
    }
}
