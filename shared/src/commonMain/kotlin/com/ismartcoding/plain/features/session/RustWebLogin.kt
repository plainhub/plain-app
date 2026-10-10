package com.ismartcoding.plain.features.session

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.events.HConfirmToAcceptLoginEvent
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.preferences.Prefs
import kotlinx.serialization.json.jsonObject

object RustWebLogin {
    private suspend fun call(command: WebLoginCommand) = RustContentApi.postJsonOrThrow("system/ws-login", JsonHelper.jsonEncodeToElement<WebLoginCommand>(command).jsonObject)
    suspend fun cancel(requestId: String) { call(WebLoginCommand.Cancel(requestId)) }
    suspend fun resetPassword(): String {
        return com.ismartcoding.plain.preferences.RustSystemState.resetPassword()
    }
    suspend fun complete(event: HConfirmToAcceptLoginEvent) { call(WebLoginCommand.Complete(event.requestId)) }
}
