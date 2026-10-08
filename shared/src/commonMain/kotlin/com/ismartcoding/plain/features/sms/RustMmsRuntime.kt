package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.coIO
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject

internal object RustMmsRuntime {
    @Serializable private data class Command(val action: String)
    fun cancelAll() { coIO { RustContentApi.postJsonOrThrow("system/mms-runtime", JsonHelper.jsonEncodeToElement(Command("cancelAll")).jsonObject) } }
}
