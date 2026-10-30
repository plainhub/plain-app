package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.state.launchSafe
import org.jetbrains.compose.resources.stringResource

@Composable
fun RowScope.RequestDecisionActions(onDeny: suspend () -> Unit, onAllow: suspend () -> Unit) {
    var pendingDecision by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    POutlinedButton(
        modifier = Modifier.weight(1f),
        text = stringResource(Res.string.deny),
        buttonSize = ButtonSize.LARGE,
        type = ButtonType.DANGER,
        isLoading = pendingDecision == false,
        enabled = pendingDecision == null,
        onClick = {
            pendingDecision = false
            scope.launchSafe(onDone = { pendingDecision = null }) { onDeny() }
        },
    )
    PFilledButton(
        modifier = Modifier.weight(1f),
        text = stringResource(Res.string.allow),
        buttonSize = ButtonSize.LARGE,
        isLoading = pendingDecision == true,
        enabled = pendingDecision == null,
        onClick = {
            pendingDecision = true
            scope.launchSafe(onDone = { pendingDecision = null }) { onAllow() }
        },
    )
}
