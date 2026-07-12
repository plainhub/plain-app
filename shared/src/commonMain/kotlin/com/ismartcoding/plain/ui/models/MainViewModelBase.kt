package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.enums.HttpServerState

/**
 * Common interface for the main view-model surface used by commonMain pages.
 *
 * The actual `MainViewModel` is platform-specific (Android) because it depends
 * on `Context` for HTTP server service management. Pages in commonMain use this
 * Context-free interface instead.
 */
interface MainViewModelBase {
    val httpServerError: MutableState<String>
    val httpServerState: MutableState<HttpServerState>
    val isVPNConnected: MutableState<Boolean>
    val ip4s: MutableState<List<String>>
    val ip4: MutableState<String>

    /** Enable/disable the embedded HTTP server. */
    fun enableHttpServer(enable: Boolean)

    /** Re-check and sync the HTTP server state (e.g. on page load). */
    fun syncHttpServerState()
}
