package com.ismartcoding.plain.ui.page.home

import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.nav.Routing

@Composable
fun HomeQuickMenuItems(
    navController: NavHostController,
    keepScreenOn: Boolean,
    onKeepScreenOnClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    PDropdownMenuItem(
        text = { Text(stringResource(R.string.scan_qrcode)) },
        leadingIcon = {
            Icon(painterResource(R.drawable.scan_qr_code), contentDescription = null)
        },
        onClick = {
            onDismiss()
            navController.navigate(Routing.Scan)
        },
    )
    PDropdownMenuItem(
        text = { Text(stringResource(R.string.keep_screen_on)) },
        leadingIcon = {
            Checkbox(checked = keepScreenOn, onCheckedChange = null)
        },
        onClick = {
            onKeepScreenOnClick()
        },
    )
    PDropdownMenuItem(
        text = { Text(stringResource(R.string.settings)) },
        leadingIcon = {
            Icon(painterResource(R.drawable.settings), contentDescription = null)
        },
        onClick = {
            onDismiss()
            navController.navigate(Routing.Settings)
        },
    )
    PDropdownMenuItem(
        text = { Text(stringResource(R.string.customize_home_features)) },
        leadingIcon = {
            Icon(painterResource(R.drawable.tune), contentDescription = null)
        },
        onClick = {
            onDismiss()
            navController.navigate(Routing.CustomFeatures)
        },
    )

}
