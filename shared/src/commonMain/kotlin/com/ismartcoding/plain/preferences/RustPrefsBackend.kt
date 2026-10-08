package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.IODispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

internal object RustPrefsBackend : PrefsBackend {
    override fun open(systemPath: String, userPath: String) = RustPrefsBridge.open(systemPath, userPath)
    override fun systemSnapshot(): String = call(PrefsCommand.Snapshot(false))?.toString() ?: RustPrefsBridge.systemSnapshot()
    override fun userSnapshot(): String = call(PrefsCommand.Snapshot(true))?.toString() ?: RustPrefsBridge.userSnapshot()
    override fun set(isUserPref: Boolean, key: String, valueJson: String) {
        if (call(PrefsCommand.Set(isUserPref, key, JsonHelper.jsonDecode(valueJson))) != null) return
        if (isUserPref) RustPrefsBridge.setUser(key, valueJson) else RustPrefsBridge.setSystem(key, valueJson)
    }
    override fun remove(isUserPref: Boolean, key: String) {
        if (call(PrefsCommand.Remove(isUserPref, key)) != null) return
        if (isUserPref) RustPrefsBridge.removeUser(key) else RustPrefsBridge.removeSystem(key)
    }
    private fun call(command: PrefsCommand): JsonObject? {
        val session = RustContentApi.startedSession() ?: return null
        return runBlocking(IODispatcher) {
            RustContentApi.postJson("system/preferences", JsonHelper.jsonEncodeToElement<PrefsCommand>(command).jsonObject, session = session).getOrThrow()
        }
    }
}
