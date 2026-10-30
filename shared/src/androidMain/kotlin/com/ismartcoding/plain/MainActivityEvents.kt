package com.ismartcoding.plain

import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.snapshotFlow
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.app.AppHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.distinctUntilChanged
import com.ismartcoding.plain.features.dlna.DlnaRendererState

internal fun MainActivity.initEvents() {
    lifecycleScope.launch {
        merge(
            snapshotFlow { MainViewModel.pendingPairingRequest.value }.map { it != null },
            snapshotFlow { MainViewModel.pendingChannelInvite.value }.map { it != null },
            snapshotFlow { MainViewModel.pendingLoginRequest.value }.map { it != null },
            snapshotFlow { MainViewModel.pendingWebAccessRequest.value }.map { it != null },
            DlnaRendererState.pendingCastRequest.map { it != null }.distinctUntilChanged(),
        ).collect { hasRequest ->
            if (hasRequest && !AppHelper.foregrounded()) openNew()
        }
    }
    lifecycleScope.launch {
        Channel.sharedFlow.collect { event ->
            if (isDestroyed || isFinishing) return@collect

            when (event) {
                is PickFileEvent -> handlePickFileEvent(event)
                is ExportFileEvent -> handleExportFileEvent(event)

            }
        }
    }
}

