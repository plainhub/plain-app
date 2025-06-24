package com.ismartcoding.plain.ui.page.root.topbars

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.ui.base.ActionButtonFolders
import com.ismartcoding.plain.ui.nav.Routing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarChat(
    navController: NavHostController
) {
    TopAppBar(
        title = {
            Text(stringResource(R.string.chat))
        },
        actions = {
            ActionButtonFolders {
                navController.navigate(Routing.Files(FilesType.APP.ordinal))
            }
            IconButton(
                onClick = {
                    navController.navigate(Routing.Nearby)
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.plus),
                    contentDescription = stringResource(R.string.add_device)
                )
            }
        }
    )
} 