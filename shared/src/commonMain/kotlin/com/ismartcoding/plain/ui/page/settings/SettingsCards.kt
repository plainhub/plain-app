package com.ismartcoding.plain.ui.page.settings
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.nav.Routing

@Composable
internal fun DeveloperSettingsCard(
    navController: NavHostController,
    awareSupported: Boolean,
    awareDataInterfaces: Int?,
    awareDataPaths: Int?,
) {
    PCard {
        PListItem(title = stringResource(Res.string.client_id), value = TempData.clientId)
        if (awareSupported) {
            PListItem(
                title = stringResource(Res.string.aware_data_interfaces),
                value = awareDataInterfaces?.toString()
                    ?: stringResource(Res.string.not_available),
            )
            PListItem(
                title = stringResource(Res.string.aware_data_paths),
                value = awareDataPaths?.toString()
                    ?: stringResource(Res.string.not_available),
            )
        }
        PListItem(
            modifier = Modifier.clickable { navController.navigate(Routing.WebDev) },
            title = stringResource(Res.string.developer_options),
            icon = Res.drawable.code,
            showMore = true,
        )
        PListItem(
            modifier = Modifier.clickable { navController.navigate(Routing.ComponentShowcase) },
            title = stringResource(Res.string.ui_components),
            icon = Res.drawable.layout_grid,
            showMore = true,
        )
        PListItem(
            modifier = Modifier.clickable { throw RuntimeException("Test crash triggered from Developer Options") },
            title = stringResource(Res.string.simulate_crash),
            subtitle = stringResource(Res.string.simulate_crash_desc),
            icon = Res.drawable.circle_alert,
        )
    }
}
