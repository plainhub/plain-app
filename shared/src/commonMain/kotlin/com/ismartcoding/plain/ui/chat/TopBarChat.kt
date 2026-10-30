package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.i18n.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.components.ActionButtonAddWithMenu
import com.ismartcoding.plain.ui.components.ActionButtonFolders
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.navigation.DeviceTopAppBar
import com.ismartcoding.plain.ui.navigation.LocalDeviceNavigation
import com.ismartcoding.plain.ui.navigation.LocalContentSource
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.components.ActionButtonSettings
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.navigation.navigateAppFiles
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.hash as ui_drawable_hash
import com.ismartcoding.plain.ui.resources.plus as ui_drawable_plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarChat(
    navController: NavHostController,
    onCreateChannel: () -> Unit,
) {
    val deviceNavigation = LocalDeviceNavigation.current
    val local = LocalContentSource.current is ContentSource.Local
    DeviceTopAppBar(
        fallbackTitle = stringResource(Res.string.chat),
        actions = {
            ActionButtonFolders {
                navController.navigateAppFiles()
            }
            ActionButtonAddWithMenu { dismiss ->
                PDropdownMenuItem(
                    text = { Text(stringResource(Res.string.create_channel)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(UiRes.drawable.ui_drawable_hash),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        dismiss()
                        onCreateChannel()
                    },
                )
                if (local) PDropdownMenuItem(
                    text = { Text(stringResource(Res.string.pair_device)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(UiRes.drawable.ui_drawable_plus),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        dismiss()
                        navController.navigate(Routing.Nearby)
                    },
                )
            }
            deviceNavigation?.let { ActionButtonSettings(it.onSettings) }
        }
    )
}
