package com.ismartcoding.plain.features.session

import com.ismartcoding.plain.api.RustContentApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.jsonObject
import com.ismartcoding.plain.lib.JsonHelper

val onlineClientIds = MutableStateFlow<Set<String>>(emptySet())

internal fun setOnlineClientIds(ids: Set<String>) { onlineClientIds.value = ids }

internal suspend fun refreshOnlineClientIds() {
    val result = RustContentApi.postJsonOrThrow("system/ws-runtime", JsonHelper.jsonEncodeToElement<WebSocketCommand>(WebSocketCommand.Snapshot).jsonObject)
    setOnlineClientIds(JsonHelper.jsonDecodeFromElement<Set<String>>(result.getValue("result")))
}

suspend fun closeAllWsSessions() {
    RustContentApi.postJsonOrThrow("system/ws-runtime", JsonHelper.jsonEncodeToElement<WebSocketCommand>(WebSocketCommand.CloseAll).jsonObject)
    onlineClientIds.value = emptySet()
}
