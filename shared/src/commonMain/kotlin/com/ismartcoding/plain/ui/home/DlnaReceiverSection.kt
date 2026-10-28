package com.ismartcoding.plain.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.dlna_receiver
import com.ismartcoding.plain.i18n.dlna_receiver_desc
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.theme.plainColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun DlnaReceiverSection(navController: NavHostController) {
    val dlnaReceiverEnabled = Prefs.dlna.collectAsStateValue()
    val scope = rememberCoroutineScope()

    PListItem(
        modifier = Modifier.clickable { navController.navigate(Routing.DlnaReceiver) },
        title = stringResource(Res.string.dlna_receiver),
        subtitle = stringResource(Res.string.dlna_receiver_desc),
    ) {
        Switch(colors = SwitchDefaults.plainColors(), checked = dlnaReceiverEnabled, onCheckedChange = { enable ->
            scope.launchSafe { Prefs.set(PrefKeys.DLNA, enable) }
        })
    }
}
