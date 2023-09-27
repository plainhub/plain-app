package com.ismartcoding.plain.ui.components.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Router
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.GridItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.box.AddNewBoxDialog
import com.ismartcoding.plain.ui.box.BoxDetailDialog
import com.ismartcoding.plain.ui.models.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeItemHardware(
    navController: NavHostController,
    itemWidth: Dp,
    viewModel: MainViewModel,
) {
    val boxesState by viewModel.boxes.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetch()
    }

    Column {
        Subtitle(
            text = stringResource(R.string.home_item_title_hardware),
        )
        HomeItemFlow {
            boxesState.forEach {
                GridItem(
                    icon = Icons.Outlined.Router,
                    it.name,
                    modifier =
                        Modifier
                            .width(itemWidth),
                ) {
                    BoxDetailDialog(it.id).show()
                }
            }
            GridItem(
                icon = Icons.Outlined.Add,
                stringResource(id = R.string.add_new_box_button),
                modifier =
                    Modifier
                        .width(itemWidth),
            ) {
                AddNewBoxDialog {
                }.show()
            }
        }
    }
}
