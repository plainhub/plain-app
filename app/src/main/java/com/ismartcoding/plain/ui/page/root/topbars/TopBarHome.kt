package com.ismartcoding.plain.ui.page.root.topbars

import android.os.Build
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.Version
import com.ismartcoding.plain.data.toVersion
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.helpers.ScreenHelper
import com.ismartcoding.plain.preferences.LocalKeepScreenOn
import com.ismartcoding.plain.preferences.LocalNewVersion
import com.ismartcoding.plain.preferences.LocalSkipVersion
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSettings
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarHome(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keepScreenOn = LocalKeepScreenOn.current
    val currentVersion = Version(BuildConfig.VERSION_NAME)
    val newVersion = LocalNewVersion.current.toVersion()
    val skipVersion = LocalSkipVersion.current.toVersion()

    PTopAppBar(
        navController = navController,
        navigationIcon = {
            ActionButtonSettings(showBadge = AppFeatureType.CHECK_UPDATES.has() && newVersion.whetherNeedUpdate(currentVersion, skipVersion)) {
                navController.navigate(Routing.Settings)
            }
        },
        title = stringResource(id = R.string.app_name) + if (BuildConfig.DEBUG) " - DEBUG" else "",
        actions = {
            ActionButtonMoreWithMenu { dismiss ->
                PDropdownMenuItem(
                    text = { Text(stringResource(R.string.keep_screen_on)) },
                    leadingIcon = {
                        Checkbox(
                            checked = keepScreenOn,
                            onCheckedChange = null
                        )
                    }, onClick = {
                        dismiss()
                        scope.launch(Dispatchers.IO) {
                            ScreenHelper.keepScreenOnAsync(context, !keepScreenOn)
                        }
                    })
                PDropdownMenuItem(leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.scan_qr_code),
                        contentDescription = stringResource(id = R.string.scan_qrcode)
                    )
                }, onClick = {
                    dismiss()
                    navController.navigate(Routing.Scan)
                }, text = {
                    Text(text = stringResource(R.string.scan_qrcode))
                })
            }
        },
    )
}