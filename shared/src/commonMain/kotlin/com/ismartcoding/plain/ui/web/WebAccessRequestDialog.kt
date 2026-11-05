package com.ismartcoding.plain.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withResumed
import com.ismartcoding.plain.features.permissions.getWebList
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.components.ConfirmDialog
import com.ismartcoding.plain.ui.components.DialogHelper
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WebAccessRequestDialog() {
    val scope = rememberCoroutineScope()
    val request = MainViewModel.pendingWebAccessRequest.value ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val dismissRequest = {
        if (MainViewModel.pendingWebAccessRequest.value === request) MainViewModel.pendingWebAccessRequest.value = null
    }
    val options = getWebList() + buildList {
        if (AppFeatureType.NOTIFICATIONS.has()) add(AccessItem(null, setOf(com.ismartcoding.plain.features.web.AccessFeature.NOTIFICATIONS)))
        add(AccessItem(null, setOf(com.ismartcoding.plain.features.web.AccessFeature.CLIPBOARD)))
    }
    val selected = options.find { request.feature in it.features }
    val message = selected?.let { stringResource(Res.string.web_access_request_message, it.getText()) }
        ?: stringResource(Res.string.not_supported_error)
    LaunchedEffect(request, selected, message, lifecycle) {
        lifecycle.withResumed { }
        snapshotFlow { DialogHelper.confirmation.value }.first { it == null }
        val permission = selected
        val dialog = ConfirmDialog(
            title = getString(Res.string.web_access_request),
            message = message,
            confirmButton = Pair(getString(if (permission == null) Res.string.ok else Res.string.grant_access)) {
                dismissRequest()
                if (permission != null) toggleAccess(scope, permission, true)
            },
            dismissButton = Pair(getString(Res.string.reject), dismissRequest),
        )
        DialogHelper.confirmation.value = dialog
        try {
            snapshotFlow { DialogHelper.confirmation.value }.first { it !== dialog }
            if (MainViewModel.pendingWebAccessRequest.value === request) MainViewModel.pendingWebAccessRequest.value = null
        } finally {
            if (DialogHelper.confirmation.value === dialog) DialogHelper.confirmation.value = null
        }
    }
}
