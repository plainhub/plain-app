package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.platform.prefsFilePath
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object Prefs {
    fun load() {
        val directory = prefsFilePath().substringBeforeLast('/')
        RustPrefsBridge.open("$directory/system_prefs.json", "$directory/user_prefs.json")
    }
    val systemSnapshot: Map<String, JsonElement> get() = runBlocking(IODispatcher) {
        RustContentApi.query("systemPrefs").getValue("systemPrefs").jsonObject
    }
    fun systemString(key: String): String? = systemSnapshot[key]?.let { JsonHelper.jsonDecodeFromElement(it) }
    fun setSystemString(key: String, value: String) = runBlocking(IODispatcher) {
        RustContentApi.graphql("mutation(\$key: String!, \$value: JSON!) { setSystemPref(key: \$key, value: \$value) { affectedCount } }",
            mapOf("key" to JsonHelper.jsonEncodeToElement(key), "value" to JsonHelper.jsonEncodeToElement(value)))
    }
    fun removeSystemPref(key: String) = runBlocking(IODispatcher) {
        RustContentApi.graphql("mutation(\$key: String!) { removeSystemPref(key: \$key) { affectedCount } }", mapOf("key" to JsonHelper.jsonEncodeToElement(key)))
    }
}
