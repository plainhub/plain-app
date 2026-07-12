package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.enums.HttpServerState

/**
 * iOS stub [MainViewModelBase]. The HTTP server currently relies on
 * Android-specific ktor bindings; on iOS enabling it is a no-op.
 */
class IosMainViewModel : ViewModel(), MainViewModelBase {
    override val httpServerError: MutableState<String> = mutableStateOf("")
    override val httpServerState: MutableState<HttpServerState> = mutableStateOf(HttpServerState.OFF)
    override val isVPNConnected: MutableState<Boolean> = mutableStateOf(false)
    override val ip4s: MutableState<List<String>> = mutableStateOf(emptyList())
    override val ip4: MutableState<String> = mutableStateOf("")

    override fun enableHttpServer(enable: Boolean) {
        // TODO: implement HTTP server on iOS
    }

    override fun syncHttpServerState() {
        // TODO: implement HTTP server on iOS
    }
}
