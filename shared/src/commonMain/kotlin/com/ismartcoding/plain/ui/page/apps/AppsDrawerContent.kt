package com.ismartcoding.plain.ui.page.apps

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.SidebarItem
import com.ismartcoding.plain.ui.models.AppsViewModel
import com.ismartcoding.plain.ui.models.VTabData

/**
 * Drawer content for the apps page: All / System / User app filters with counts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsDrawerContent(appsVM: AppsViewModel, onSelect: (VTabData) -> Unit) {
    val tabIcons = listOf(Res.drawable.layout_grid, Res.drawable.package2, Res.drawable.rocket)
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(NavigationDrawerItemDefaults.ItemPadding)
    ) {
        VerticalSpace(dp = 16.dp)
        appsVM.tabs.value.forEachIndexed { index, tab ->
            SidebarItem(
                label = tab.title,
                icon = tabIcons.getOrElse(index) { Res.drawable.layout_grid },
                isSelected = appsVM.appType.value == tab.value,
                onClick = { onSelect(tab) },
                badge = tab.count.toString()
            )
        }
        BottomSpace()
    }
}
