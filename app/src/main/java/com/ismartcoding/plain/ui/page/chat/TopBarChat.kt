package com.ismartcoding.plain.ui.page.chat

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.ActionButtonAddWithMenu
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.ActionButtonFolders
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.nav.navigateAppFiles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarChat(
    navController: NavHostController,
    channelVM: ChannelViewModel,
    onNavigateBack: () -> Unit,
) {
    val showMenu = remember { mutableStateOf(false) }

    PTopAppBar(
        navController = navController,
        navigationIcon = {
            NavigationBackIcon(onClick = onNavigateBack)
        },
        title = stringResource(R.string.chat),
        actions = {
            ActionButtonFolders {
                navController.navigateAppFiles()
            }
            ActionButtonAddWithMenu { dismiss ->
                PDropdownMenuItem(
                    text = { Text(stringResource(R.string.new_channel)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.hash),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        dismiss()
                        channelVM.showCreateChannelDialog.value = true
                    },
                )
                PDropdownMenuItem(
                    text = { Text(stringResource(R.string.add_device)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.plus),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        dismiss()
                        navController.navigate(Routing.Nearby())
                    },
                )
            }
        }
    )
} 