package com.ismartcoding.plain.ui.page.dlna

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.features.dlna.receiver.DlnaReceiverEngine
import com.ismartcoding.plain.features.dlna.startDlnaRenderer
import com.ismartcoding.plain.preferences.DlnaPreference
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.launchSafe
import com.ismartcoding.plain.ui.nav.Routing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DlnaReceiverPage(navController: NavHostController) {
    val enabled by TempData.dlnaEnabled.collectAsState()
    val startError by DlnaRendererState.startError.collectAsState()
    val isRetrying by DlnaRendererState.isRetrying.collectAsState()

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.dlna_receiver),
                actions = {
                    PIconButton(
                        icon = Res.drawable.history,
                        contentDescription = stringResource(Res.string.dlna_cast_history),
                        tint = MaterialTheme.colorScheme.onSurface
                    ) { navController.navigate(Routing.DlnaCastHistory) }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (startError.isNotEmpty()) {
                item {
                    PAlert(
                        description = startError,
                        type = AlertType.ERROR,
                        actions = {
                            PFilledButton(
                                stringResource(Res.string.retry),
                                buttonSize = ButtonSize.SMALL,
                                isLoading = isRetrying,
                                onClick = { DlnaReceiverEngine.retry() }
                            )
                        },
                    )
                }
            }
            item {
                if (enabled) {
                    DlnaReceiverWaitingScreen()
                } else {
                    DlnaReceiverDisabledScreen()
                }
            }
            item { BottomSpace() }
        }
    }
}

@Composable
private fun DlnaReceiverDisabledScreen() {
    val scope = rememberCoroutineScope()
    PCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.feature_disabled_title, stringResource(Res.string.dlna_receiver)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.dlna_receiver_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PFilledButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.dlna_receiver_turn_on),
                onClick = {
                    scope.launchSafe {
                        DlnaPreference.putAsync(true)
                        startDlnaRenderer()
                    }
                },
                buttonSize = ButtonSize.EXTRA_LARGE,
            )
        }
    }
}
