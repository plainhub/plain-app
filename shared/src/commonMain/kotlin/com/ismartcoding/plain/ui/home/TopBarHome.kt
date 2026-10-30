package com.ismartcoding.plain.ui.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.services
import com.ismartcoding.plain.ui.components.ActionButtonScan
import com.ismartcoding.plain.ui.navigation.DeviceTopAppBar
import com.ismartcoding.plain.ui.navigation.Routing
import org.jetbrains.compose.resources.stringResource

@Composable
fun TopBarHome(navController: NavHostController) {
    DeviceTopAppBar(
        fallbackTitle = stringResource(Res.string.services),
        actions = { ActionButtonScan { navController.navigate(Routing.Scan) } },
    )
}
