package com.ismartcoding.plain.ui.page.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.dlna_receiver
import com.ismartcoding.plain.i18n.dlna_receiver_desc
import com.ismartcoding.plain.features.dlna.startDlnaRenderer
import com.ismartcoding.plain.features.dlna.stopDlnaRenderer
import com.ismartcoding.plain.i18n.cast
import com.ismartcoding.plain.preferences.DlnaPreference
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.launchSafe
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.tipsText
import org.jetbrains.compose.resources.stringResource

@Composable
fun DlnaReceiverSection(navController: NavHostController) {
    val dlnaReceiverEnabled = TempData.dlnaEnabled.collectAsStateValue()
    val scope = rememberCoroutineScope()

    PCard {
        PListItem(
            modifier = Modifier.clickable { navController.navigate(Routing.DlnaReceiver) },
            icon = Res.drawable.cast,
            separatedActions = true,
            title = stringResource(Res.string.dlna_receiver),
        ) {
            PSwitch(activated = dlnaReceiverEnabled) { enable ->
                scope.launchSafe {
                    DlnaPreference.putAsync(enable)
                    if (enable) startDlnaRenderer() else stopDlnaRenderer()
                }
            }
            HorizontalSpace(8.dp)
        }
        Text(
            text = stringResource(Res.string.dlna_receiver_desc),
            style = MaterialTheme.typography.tipsText(),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        VerticalSpace(16.dp)
    }
}
