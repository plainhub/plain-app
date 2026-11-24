package com.ismartcoding.plain.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.platform.system.getDeviceName
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.ActionButtonScan
import com.ismartcoding.plain.ui.components.ActionButtonSettings
import com.ismartcoding.plain.ui.navigation.Routing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarHome(navController: NavHostController) {
    val localName by Prefs.deviceName.collectAsState()
    PTopAppBar(
        title = localName.ifEmpty { getDeviceName() },
        actions = {
            ActionButtonScan { navController.navigate(Routing.Scan) }
            ActionButtonSettings { navController.navigate(Routing.Settings) }
        },
    )
}
