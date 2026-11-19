package com.ismartcoding.plain

import com.ismartcoding.plain.features.settings.ServiceState
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.http_server_failed
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.RustHttpStatus
import com.ismartcoding.plain.rust.native.RustNativeApi
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

object HttpServerManager {
    val httpServerState = MutableStateFlow(ServiceState.STOPPED)
    val httpServerError = MutableStateFlow("")
    val httpServerResourcesError = MutableStateFlow("")
    val foregroundServiceError = MutableStateFlow("")

    private suspend fun setHttpServerError(detail: String) {
        LogCat.e("HTTP server failed: $detail")
        httpServerError.value = detail.ifEmpty { LocaleHelper.getStringAsync(Res.string.http_server_failed) }
    }

    internal suspend fun applyHttpServerStatus(status: RustHttpStatus) {
        httpServerState.value = status.state
        if (status.state == ServiceState.ERROR || status.message.isNotEmpty()) setHttpServerError(status.message)
        else httpServerError.value = ""
    }

    fun restartHttpServer() {
        coIO {
            try {
                RustNativeApi.restartHttpServer()
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (error: Exception) {
                setHttpServerError(error.message.orEmpty())
            }
        }
    }
}
