package com.ismartcoding.plain.features.dlna

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.jsonObject

internal object RustDlnaRules {
    suspend fun snapshot() = call(ReceiverCommand.Rules)
    suspend fun remove(ip: String, allowed: Boolean) = call(ReceiverCommand.RemoveRule(ip, allowed))
    private suspend fun call(command: ReceiverCommand): DlnaRulesSnapshot {
        val response = RustContentApi.postJsonOrThrow("dlna/receiver", JsonHelper.jsonEncodeToElement<ReceiverCommand>(command).jsonObject)
        return JsonHelper.jsonDecodeFromElement<DlnaRulesSnapshot>(response.getValue("result"))
    }
}
