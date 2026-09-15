package com.ismartcoding.plain.ui.page

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.chat
import com.ismartcoding.plain.i18n.grid_3x3
import com.ismartcoding.plain.i18n.home
import com.ismartcoding.plain.i18n.house
import com.ismartcoding.plain.i18n.message_circle
import com.ismartcoding.plain.i18n.tools
import com.ismartcoding.plain.platform.isAndroidOnly
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.theme.primaryPill
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal data class MainNavItem(
    val index: Int,
    val icon: DrawableResource,
    val label: StringResource,
)

internal val mainNavItems: List<MainNavItem>
    get() = buildList {
        add(MainNavItem(0, Res.drawable.house, Res.string.home))
        add(MainNavItem(1, Res.drawable.message_circle, Res.string.chat))
        if (isAndroidOnly()) {
            add(MainNavItem(2, Res.drawable.grid_3x3, Res.string.tools))
        }
    }

// Material 3 window width class breakpoints: compact < 600dp <= medium < 840dp <= expanded.
private val MediumNavMinWidth = 600.dp
private val ExpandedNavMinWidth = 840.dp

/**
 * Scaffold for the root tab pages with navigation that adapts to the window
 * width: bottom bar (compact), navigation rail (medium) or permanent
 * navigation drawer (expanded). Pass `onTabSelected = null` when the page is
 * hosted outside the main tab layout — it renders a plain scaffold.
 */
@Composable
fun MainNavScaffold(
    selectedIndex: Int,
    onTabSelected: ((Int) -> Unit)?,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    if (onTabSelected == null) {
        PScaffold(topBar = topBar, content = content)
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // The rail/drawer consume the start-side window insets for themselves,
        // but sibling content still reads the raw window insets — report start
        // as consumed so the inner Scaffold and the pages' TopAppBar don't pad
        // it a second time (visible as a blank strip before the title).
        val consumedStart = WindowInsets.systemBars.only(WindowInsetsSides.Start)
        when {
            maxWidth >= ExpandedNavMinWidth -> {
                PermanentNavigationDrawer(
                    drawerContent = { MainNavDrawerContent(selectedIndex, onTabSelected) },
                ) {
                    PScaffold(
                        modifier = Modifier.consumeWindowInsets(consumedStart),
                        topBar = topBar,
                        content = content,
                    )
                }
            }
            maxWidth >= MediumNavMinWidth -> {
                Row(Modifier.fillMaxSize()) {
                    MainNavRail(selectedIndex, onTabSelected)
                    PScaffold(
                        modifier = Modifier.weight(1f).consumeWindowInsets(consumedStart),
                        topBar = topBar,
                        content = content,
                    )
                }
            }
            else -> {
                PScaffold(
                    topBar = topBar,
                    bottomBar = { MainBottomBar(selectedIndex, onTabSelected) },
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun MainNavRail(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.background) {
        mainNavItems.forEach { item ->
            NavigationRailItem(
                selected = selectedIndex == item.index,
                onClick = { onTabSelected(item.index) },
                icon = {
                    Icon(painterResource(item.icon), contentDescription = null)
                },
                label = { Text(stringResource(item.label)) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryPill,
                ),
            )
        }
    }
}

@Composable
private fun MainNavDrawerContent(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    PermanentDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background) {
        Spacer(Modifier.height(16.dp))
        mainNavItems.forEach { item ->
            NavigationDrawerItem(
                label = { Text(stringResource(item.label)) },
                selected = selectedIndex == item.index,
                onClick = { onTabSelected(item.index) },
                icon = {
                    Icon(painterResource(item.icon), contentDescription = null)
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryPill,
                ),
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}
