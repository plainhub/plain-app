package com.ismartcoding.plain.ui.page.root.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.navBarBackground
import com.ismartcoding.plain.ui.theme.navBarUnselectedColor

@Composable
fun RootNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navBarColor = MaterialTheme.colorScheme.navBarBackground
    val unselectedColor = MaterialTheme.colorScheme.navBarUnselectedColor

    data class NavItem(val tab: Int, val icon: Int, val label: Int)

    val items = listOf(
        NavItem(RootTabType.HOME.value, R.drawable.house, R.string.home),
        NavItem(RootTabType.CHAT.value, R.drawable.message_circle, R.string.chat),
        NavItem(RootTabType.AUDIO.value, R.drawable.music, R.string.audios),
        NavItem(RootTabType.IMAGES.value, R.drawable.image, R.string.images),
        NavItem(RootTabType.VIDEOS.value, R.drawable.video, R.string.videos),
    )

    NavigationBar(
        modifier = modifier,
        containerColor = navBarColor,
    ) {
        items.forEach { item ->
            val selected = selectedTab == item.tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = stringResource(item.label),
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.label),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor,
                ),
            )
        }
    }
}