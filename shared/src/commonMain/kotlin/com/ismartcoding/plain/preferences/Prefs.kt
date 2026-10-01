package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.platform.Locale
import com.ismartcoding.plain.platform.PlatformLock
import com.ismartcoding.plain.platform.prefsFilePath
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer

object Prefs {
    internal val json = Json { ignoreUnknownKeys = true }
    private val lock = PlatformLock()
    private val fields = mutableListOf<PrefFlow<*>>()
    private var systemEntries: Map<String, JsonElement> = emptyMap()
    private var userEntries: Map<String, JsonElement> = emptyMap()
    private var loaded = false
    private var backend: PrefsBackend = RustPrefsBackend

    internal inline fun <reified T> systemFlow(key: String, default: T): PrefFlow<T> =
        PrefFlow(key, false, default, serializer<T>()).also { fields.add(it) }

    internal inline fun <reified T> userFlow(key: String, default: T): PrefFlow<T> =
        PrefFlow(key, true, default, serializer<T>()).also { fields.add(it) }

    internal fun <T> userFlow(key: String, default: T, serializer: KSerializer<T>): PrefFlow<T> =
        PrefFlow(key, true, default, serializer).also { fields.add(it) }

    fun load() {
        val directory = prefsFilePath().substringBeforeLast('/')
        load("$directory/system_prefs.json", "$directory/user_prefs.json", RustPrefsBackend)
    }

    internal fun load(systemPath: String, userPath: String, storage: PrefsBackend) {
        SystemPrefs.initialize()
        UserPrefs.initialize()
        lock.withLock {
            storage.open(systemPath, userPath)
            val storedSystem = (json.parseToJsonElement(storage.systemSnapshot()) as JsonObject).toMap()
            val storedUser = (json.parseToJsonElement(storage.userSnapshot()) as JsonObject).toMap()
            backend = storage
            systemEntries = storedSystem
            userEntries = storedUser
            fields.forEach { field ->
                if (!field.dirty) {
                    field.restore((if (field.isUserPref) storedUser else storedSystem)[field.key])
                }
            }
            loaded = true
            saveLocked()
        }
    }

    fun save() = lock.withLock { saveLocked() }

    internal fun <T> update(field: PrefFlow<T>, value: T) = lock.withLock { updateLocked(field, value) }

    internal fun <T> compareAndSet(field: PrefFlow<T>, expect: T, value: T): Boolean = lock.withLock {
        if (field.value != expect) return@withLock false
        updateLocked(field, value)
        true
    }

    val systemSnapshot: Map<String, JsonElement> get() = lock.withLock { systemEntries }
    val userSnapshot: Map<String, JsonElement> get() = lock.withLock { userEntries }

    fun systemString(key: String): String? = systemSnapshot[key]?.let {
        runCatching { json.decodeFromJsonElement<String>(it) }.getOrNull()
    }

    fun setSystemString(key: String, value: String) =
        write(false, key, json.encodeToJsonElement(value))

    fun setUserPref(key: String, value: JsonElement) = lock.withLock { writeLocked(true, key, value) }
    fun setSystemPref(key: String, value: JsonElement) = lock.withLock { writeLocked(false, key, value) }
    fun removeUserPref(key: String) = remove(true, key)
    fun removeSystemPref(key: String) = remove(false, key)

    internal inline fun <reified T> decodeOrDefault(value: String, default: () -> T): T =
        if (value.isEmpty()) default() else runCatching { json.decodeFromString<T>(value) }.getOrElse { default() }

    fun parseLocale(value: String): Locale? {
        if (value.isEmpty()) return null
        val parts = value.split("-")
        return if (parts.size > 1) Locale(parts[0], parts[1]) else Locale(value, "")
    }

    fun encodeSenderEntry(ip: String, name: String) = "$ip|$name"

    fun decodeSenderEntry(entry: String): Pair<String, String> {
        val index = entry.indexOf('|')
        return if (index >= 0) entry.substring(0, index) to entry.substring(index + 1) else entry to ""
    }

    internal fun senderEntriesWithout(entries: Set<String>, ip: String) =
        entries.filterNot { decodeSenderEntry(it).first == ip }.toSet()

    internal fun senderEntriesWith(entries: Set<String>, ip: String, name: String) =
        senderEntriesWithout(entries, ip) + encodeSenderEntry(ip, name)

    private fun <T> updateLocked(field: PrefFlow<T>, value: T) {
        val entries = entriesFor(field.isUserPref)
        if (field.value == value && entries[field.key] == field.encode()) return
        val previous = field.value
        field.setInMemory(value)
        try {
            saveLocked()
        } catch (error: Throwable) {
            field.setInMemory(previous)
            field.dirty = false
            throw error
        }
    }

    private fun saveLocked() {
        if (!loaded) return
        fields.forEach { field ->
            if (!field.dirty) return@forEach
            val encoded = field.encode()
            if (entriesFor(field.isUserPref)[field.key] != encoded) {
                writeLocked(field.isUserPref, field.key, encoded)
            }
            field.dirty = false
        }
    }

    private fun remove(isUserPref: Boolean, key: String) {
        lock.withLock {
            val entries = entriesFor(isUserPref)
            if (!entries.containsKey(key)) return@withLock
            backend.remove(isUserPref, key)
            setEntries(isUserPref, entries - key)
            fields.firstOrNull { it.isUserPref == isUserPref && it.key == key }?.restore(null)
        }
    }

    private fun write(isUserPref: Boolean, key: String, value: JsonElement) =
        lock.withLock { writeLocked(isUserPref, key, value) }

    private fun writeLocked(isUserPref: Boolean, key: String, value: JsonElement) {
        check(loaded) { "Preferences must be loaded before writing raw keys" }
        if (entriesFor(isUserPref)[key] == value) return
        backend.set(isUserPref, key, value.toString())
        setEntries(isUserPref, entriesFor(isUserPref) + (key to value))
        fields.firstOrNull { it.isUserPref == isUserPref && it.key == key }?.restore(value)
    }

    private fun entriesFor(isUserPref: Boolean) = if (isUserPref) userEntries else systemEntries

    private fun setEntries(isUserPref: Boolean, entries: Map<String, JsonElement>) {
        if (isUserPref) userEntries = entries else systemEntries = entries
    }

    internal fun resetForTest() = lock.withLock {
        loaded = false
        backend = RustPrefsBackend
        systemEntries = emptyMap()
        userEntries = emptyMap()
        fields.forEach { it.restore(null) }
    }
}

internal interface PrefsBackend {
    fun open(systemPath: String, userPath: String)
    fun systemSnapshot(): String
    fun userSnapshot(): String
    fun set(isUserPref: Boolean, key: String, valueJson: String)
    fun remove(isUserPref: Boolean, key: String)
}

private object RustPrefsBackend : PrefsBackend {
    override fun open(systemPath: String, userPath: String) = RustPrefsBridge.open(systemPath, userPath)
    override fun systemSnapshot(): String = RustPrefsBridge.systemSnapshot()
    override fun userSnapshot(): String = RustPrefsBridge.userSnapshot()
    override fun set(isUserPref: Boolean, key: String, valueJson: String) {
        if (isUserPref) RustPrefsBridge.setUser(key, valueJson) else RustPrefsBridge.setSystem(key, valueJson)
    }
    override fun remove(isUserPref: Boolean, key: String) {
        if (isUserPref) RustPrefsBridge.removeUser(key) else RustPrefsBridge.removeSystem(key)
    }
}
