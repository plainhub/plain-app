package com.ismartcoding.plain.ui.page.root.topbars

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.ui.base.ActionButtonFolders
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.models.ChatListViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.nav.navigateFiles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarChat(
    navController: NavHostController,
    chatListVM: ChatListViewModel,
) {
    val context = LocalContext.current
    val showMenu = remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(stringResource(R.string.chat))
        },
        actions = {
            ActionButtonFolders {
                navController.navigateFiles(FileSystemHelper.getExternalFilesDirPath(context))
            }
            IconButton(
                onClick = {
                    showMenu.value = true
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.plus),
                    contentDescription = stringResource(R.string.create)
                )
                PDropdownMenu(
                    expanded = showMenu.value,
                    onDismissRequest = { showMenu.value = false },
                ) {
                    PDropdownMenuItem(
                        text = { Text(stringResource(R.string.new_channel)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.hash),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showMenu.value = false
                            chatListVM.showCreateChannelDialog.value = true
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
                            showMenu.value = false
                            navController.navigate(Routing.Nearby())
                        },
                    )
                }
            }
        }
    )
} 