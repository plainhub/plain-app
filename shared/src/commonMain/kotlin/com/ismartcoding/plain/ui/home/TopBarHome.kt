package com.ismartcoding.plain.ui.home

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.rename
import com.ismartcoding.plain.platform.system.getDeviceName
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.ActionButtonScan
import com.ismartcoding.plain.ui.components.ActionButtonSettings
import com.ismartcoding.plain.ui.components.DeviceRenameDialog
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.pen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarHome(navController: NavHostController) {
    val localName by Prefs.deviceName.collectAsState()
    val deviceName = localName.ifEmpty { getDeviceName() }
    var showRename by remember { mutableStateOf(false) }
    if (showRename) {
        DeviceRenameDialog(name = deviceName, onDismiss = { showRename = false }) {
            GraphqlApi.execute(Mutation.updateDeviceName(it))
        }
    }
    PTopAppBar(
        title = deviceName,
        onTitleClick = { showRename = true },
        titleTrailing = {
            PIcon(
                modifier = Modifier.size(16.dp),
                icon = painterResource(UiRes.drawable.pen),
                contentDescription = stringResource(Res.string.rename),
            )
        },
        actions = {
            ActionButtonScan { navController.navigate(Routing.Scan) }
            ActionButtonSettings { navController.navigate(Routing.Settings) }
        },
    )
}
