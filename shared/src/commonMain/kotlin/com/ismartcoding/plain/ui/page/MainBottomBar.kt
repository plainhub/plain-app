package com.ismartcoding.plain.ui.page

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.chat
import com.ismartcoding.plain.i18n.grid_3x3
import com.ismartcoding.plain.i18n.home
import com.ismartcoding.plain.i18n.house
import com.ismartcoding.plain.i18n.message_circle
import com.ismartcoding.plain.i18n.tools
import com.ismartcoding.plain.platform.isAndroidOnly
import com.ismartcoding.plain.platform.isDebugBuild
import com.ismartcoding.plain.ui.theme.primaryPill
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryPill,
    )
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.house),
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(Res.string.home)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.message_circle),
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(Res.string.chat)) },
            colors = itemColors,
        )
        if (isAndroidOnly()) {
            NavigationBarItem(
                selected = selectedIndex == 2,
                onClick = { onTabSelected(2) },
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.grid_3x3),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(Res.string.tools)) },
                colors = itemColors,
            )
        }
    }
}
