package com.ismartcoding.plain.ui.page.root.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.preferences.LocalKeepScreenOn
import com.ismartcoding.plain.ui.nav.Routing

data class DrawerItem(val tab: Int, val icon: Int, val label: Int)

@Composable
fun DrawerHeader(
    deviceName: String,
    navController: NavHostController,
    onRenameClick: () -> Unit,
    onCloseDrawer: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = deviceName.ifEmpty { PhoneHelper.getDeviceName(context) },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onRenameClick) {
            Icon(
                painter = painterResource(R.drawable.square_pen),
                contentDescription = stringResource(R.string.device_name),
            )
        }
        IconButton(
            onClick = {
                onCloseDrawer()
                navController.navigate(Routing.Settings)
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.settings),
                contentDescription = stringResource(R.string.settings),
            )
        }
    }
}

@Composable
fun DrawerTabs(
    items: List<DrawerItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    items.forEach { item ->
        NavigationDrawerItem(
            label = {
                Text(
                    text = stringResource(item.label),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                )
            },
            selected = selectedTab == item.tab,
            onClick = { onTabSelected(item.tab) },
            icon = {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = stringResource(item.label),
                )
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
fun DrawerBottomActions(
    onKeepScreenOnClick: () -> Unit,
    onScanClick: () -> Unit,
) {
    val keepScreenOn = LocalKeepScreenOn.current
    NavigationDrawerItem(
        label = { Text(stringResource(R.string.keep_screen_on)) },
        selected = false,
        onClick = onKeepScreenOnClick,
        icon = {
            Checkbox(
                checked = keepScreenOn,
                onCheckedChange = null,
            )
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
    NavigationDrawerItem(
        label = { Text(stringResource(R.string.scan_qrcode)) },
        selected = false,
        onClick = onScanClick,
        icon = {
            Icon(
                painter = painterResource(R.drawable.scan_qr_code),
                contentDescription = stringResource(R.string.scan_qrcode),
            )
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}