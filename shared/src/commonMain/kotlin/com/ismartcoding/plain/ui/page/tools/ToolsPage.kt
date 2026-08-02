package com.ismartcoding.plain.ui.page.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.tools
import com.ismartcoding.plain.ui.base.ActionButtonAdd
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.MainBottomBar
import com.ismartcoding.plain.ui.page.home.HomeFeatureItemsGrid
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverPage(
    navController: NavHostController,
    onTabSelected: (Int) -> Unit,
) {
    PScaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.tools),
                actions = {
                    ActionButtonAdd {
                        navController.navigate(Routing.CustomFeatures)
                    }
                },
            )
        },
        bottomBar = { MainBottomBar(selectedIndex = 2, onTabSelected = onTabSelected) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            TopSpace()
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeFeatureItemsGrid(navController)
            }
            VerticalSpace(dp = 16.dp)
            BottomSpace(paddingValues)
        }
    }
}
