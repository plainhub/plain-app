package com.ismartcoding.plain.ui.page.root.topbars

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.preferences.ChatFilesSaveFolderPreference
import com.ismartcoding.plain.ui.base.ActionButtonFolders
import com.ismartcoding.plain.ui.base.ActionButtonSettings
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.nav.navigateFiles
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarChat(
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    TopAppBar(
        title = {
            Text(stringResource(R.string.chat))
        },
        actions = {
            ActionButtonSettings {
                navController.navigate(Routing.ChatSettings)
            }
            ActionButtonFolders {
                scope.launch {
                    val folderPath = withIO {
                        ChatFilesSaveFolderPreference.getAsync(context)
                    }
                    navController.navigateFiles(folderPath.ifEmpty {
                        FileSystemHelper.getExternalFilesDirPath(context)
                    })
                }
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