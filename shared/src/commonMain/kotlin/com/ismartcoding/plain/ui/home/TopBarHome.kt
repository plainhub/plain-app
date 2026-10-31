package com.ismartcoding.plain.ui.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.components.ActionButtonScan
import com.ismartcoding.plain.ui.navigation.DeviceTopAppBar
import com.ismartcoding.plain.ui.navigation.Routing

@Composable
fun TopBarHome(navController: NavHostController) {
    DeviceTopAppBar(
        navController = navController,
        actions = { ActionButtonScan { navController.navigate(Routing.Scan) } },
    )
}
