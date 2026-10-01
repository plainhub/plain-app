package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.Permission
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrefsTest {
    @BeforeTest
    fun before() = Prefs.resetForTest()

    @AfterTest
    fun after() = Prefs.resetForTest()

    @Test
    fun loadRestoresTypedFlowsAndPreservesUnrelatedEntries() {
        val backend = FakeBackend(
            """{"auth_two_factor":false,"api_permissions":["ADB"],"other":{"nested":1}}""",
            """{"http_port":9090,"audio_play_mode":${MediaPlayMode.SHUFFLE.ordinal}}""",
        )

        Prefs.load("system_prefs.json", "user_prefs.json", backend)

        assertEquals("system_prefs.json", backend.openedSystemPath)
        assertEquals("user_prefs.json", backend.openedUserPath)
        assertEquals(9090, UserPrefs.httpPort.value)
        assertFalse(SystemPrefs.authTwoFactor.value)
        assertEquals(setOf("ADB"), SystemPrefs.apiPermissions.value)
        assertEquals(MediaPlayMode.SHUFFLE, UserPrefs.audioPlayMode.value)
        assertEquals(8080, UserPrefs.httpPort.default)
        assertEquals(0, backend.writes)

        UserPrefs.httpPort.value = 9191

        assertEquals(JsonPrimitive(9191), backend.userEntries["http_port"])
        assertEquals(Json.parseToJsonElement("""{"nested":1}"""), backend.entries["other"])
        assertEquals(9191, UserPrefs.httpPort.value)
        assertEquals(1, backend.writes)
    }

    @Test
    fun everyStateFlowWritePathPersistsAndReloads() = runBlocking {
        val backend = FakeBackend("{}")
        Prefs.load("system_prefs.json", "user_prefs.json", backend)

        UserPrefs.httpPort.value = 8080
        assertEquals(JsonPrimitive(8080), backend.userEntries["http_port"])
        assertEquals(1, backend.writes)

        assertTrue(UserPrefs.httpPort.compareAndSet(8080, 8081))
        assertFalse(UserPrefs.httpPort.compareAndSet(8080, 9999))
        assertTrue(UserPrefs.httpPort.tryEmit(8082))
        UserPrefs.httpPort.emit(8083)
        assertEquals(4, backend.writes)
        assertEquals(JsonPrimitive(8083), backend.userEntries["http_port"])

        Prefs.resetForTest()
        Prefs.load("system_prefs.json", "user_prefs.json", backend)
        assertEquals(8083, UserPrefs.httpPort.value)
    }

    @Test
    fun invalidStoredValueUsesDefaultAndCanBeRepaired() {
        val backend = FakeBackend(
            """{"auth_two_factor":42}""",
            """{"http_port":"bad","other":"keep"}""",
        )
        Prefs.load("system_prefs.json", "user_prefs.json", backend)

        assertEquals(8080, UserPrefs.httpPort.value)
        assertTrue(SystemPrefs.authTwoFactor.value)
        assertEquals(0, backend.writes)

        UserPrefs.httpPort.value = 8080
        assertEquals(JsonPrimitive(8080), backend.userEntries["http_port"])
        assertEquals(JsonPrimitive("keep"), backend.userEntries["other"])
    }

    @Test
    fun failedWriteRestoresFlowAndLeavesStoredDataIntact() {
        val backend = FakeBackend("{}", """{"http_port":9090}""")
        Prefs.load("system_prefs.json", "user_prefs.json", backend)
        backend.failWrites = true

        assertFailsWith<IllegalStateException> { UserPrefs.httpPort.value = 9999 }

        assertEquals(9090, UserPrefs.httpPort.value)
        assertEquals(JsonPrimitive(9090), backend.userEntries["http_port"])
        backend.failWrites = false
        UserPrefs.httpPort.value = 9191
        assertEquals(JsonPrimitive(9191), backend.userEntries["http_port"])
    }

    @Test
    fun rawKeysShareTheSameSnapshotAndCanBeRemoved() {
        val backend = FakeBackend("{}", """{"http_port":9090,"theme":"light","sms.pending":"queued"}""")
        Prefs.load("system_prefs.json", "user_prefs.json", backend)

        UserPrefs.set("theme", JsonPrimitive("dark"))
        UserPrefs.httpPort.value = 9191
        UserPrefs.remove("sms.pending")

        assertEquals(JsonPrimitive("dark"), UserPrefs.snapshot["theme"])
        assertEquals(9191, UserPrefs.httpPort.value)
        assertEquals(null, UserPrefs.snapshot["sms.pending"])
        assertEquals(setOf("http_port", "theme"), backend.userEntries.keys)
        assertEquals(UserPrefs.snapshot, backend.userEntries)

        UserPrefs.remove("http_port")
        assertEquals(8080, UserPrefs.httpPort.value)
        assertFalse(backend.userEntries.containsKey("http_port"))
    }

    @Test
    fun brokenSnapshotDoesNotReplaceCurrentValues() {
        UserPrefs.httpPort.value = 9999
        val backend = FakeBackend("{}")
        backend.invalidSnapshot = true

        assertFailsWith<ClassCastException> {
            Prefs.load("system_prefs.json", "user_prefs.json", backend)
        }

        assertEquals(9999, UserPrefs.httpPort.value)
        backend.invalidSnapshot = false
        Prefs.load("system_prefs.json", "user_prefs.json", backend)
        assertEquals(JsonPrimitive(9999), backend.userEntries["http_port"])
    }

    @Test
    fun writeBeforeLoadIsFlushedAfterSnapshotIsRead() {
        UserPrefs.httpPort.value = 9999
        val backend = FakeBackend("{}", """{"http_port":9090,"https_port":9443}""")

        Prefs.load("system_prefs.json", "user_prefs.json", backend)

        assertEquals(9999, UserPrefs.httpPort.value)
        assertEquals(9443, UserPrefs.httpsPort.value)
        assertEquals(JsonPrimitive(9999), backend.userEntries["http_port"])
    }

    @Test
    fun complexSettingsAndRecentSearchesKeepTheirBehavior() {
        val backend = FakeBackend("{}")
        Prefs.load("system_prefs.json", "user_prefs.json", backend)
        val folder = DFavoriteFolder(rootPath = "/root", fullPath = "/root/music")

        UserPrefs.addFavoriteFolder(folder)
        UserPrefs.addFavoriteFolder(folder.copy(alias = "Music"))
        assertEquals(listOf("Music"), UserPrefs.favoriteFoldersValue().map { it.alias })
        assertTrue(UserPrefs.isFavoriteFolder(folder.fullPath))
        UserPrefs.removeFavoriteFolder(folder.fullPath)
        assertFalse(UserPrefs.isFavoriteFolder(folder.fullPath))

        SystemPrefs.setApiPermission(Permission.ADB, true)
        SystemPrefs.setApiPermission(Permission.CAMERA, true)
        SystemPrefs.setApiPermission(Permission.ADB, false)
        assertEquals(setOf(Permission.CAMERA.name), SystemPrefs.apiPermissions.value)

        (0..11).forEach { UserPrefs.recordRecentSearch("term$it") }
        UserPrefs.recordRecentSearch("TERM5")
        assertEquals(10, UserPrefs.recentSearchesValue().size)
        assertEquals("TERM5", UserPrefs.recentSearchesValue().first())
        assertEquals(1, UserPrefs.recentSearchesValue().count { it.equals("term5", ignoreCase = true) })

        Prefs.resetForTest()
        Prefs.load("system_prefs.json", "user_prefs.json", backend)
        assertEquals(setOf(Permission.CAMERA.name), SystemPrefs.apiPermissions.value)
        assertEquals("TERM5", UserPrefs.recentSearchesValue().first())
    }

    @Test
    fun updateNotificationAndSortHelpersRetainStoredBehavior() {
        val backend = FakeBackend("{}")
        Prefs.load("system_prefs.json", "user_prefs.json", backend)

        SystemPrefs.updateInfo { it.copy(skipVersion = "4.0", autoCheckUpdate = false) }
        assertEquals("4.0", SystemPrefs.updateInfoValue().skipVersion)
        assertFalse(SystemPrefs.updateInfoValue().autoCheckUpdate)

        UserPrefs.toggleNotificationApp("app.one")
        assertFalse(UserPrefs.isNotificationAllowed("app.one"))
        UserPrefs.setNotificationMode("allowlist")
        assertTrue(UserPrefs.isNotificationAllowed("app.one"))
        assertFalse(UserPrefs.isNotificationAllowed("app.two"))

        UserPrefs.setAudioSortBy(FileSortBy.NAME_DESC)
        assertEquals(FileSortBy.NAME_DESC, UserPrefs.audioSortByValue())
        assertEquals(JsonPrimitive(FileSortBy.NAME_DESC.ordinal), backend.userEntries["audio_sort_by"])

        Prefs.resetForTest()
        Prefs.load("system_prefs.json", "user_prefs.json", backend)
        assertEquals("4.0", SystemPrefs.updateInfoValue().skipVersion)
        assertTrue(UserPrefs.isNotificationAllowed("app.one"))
        assertEquals(FileSortBy.NAME_DESC, UserPrefs.audioSortByValue())
    }

    private class FakeBackend(systemSnapshot: String, userSnapshot: String = "{}") : PrefsBackend {
        val entries = (Json.parseToJsonElement(systemSnapshot) as JsonObject).toMutableMap()
        val userEntries = (Json.parseToJsonElement(userSnapshot) as JsonObject).toMutableMap()
        var openedSystemPath: String? = null
        var openedUserPath: String? = null
        var writes = 0
        var failWrites = false
        var invalidSnapshot = false

        override fun open(systemPath: String, userPath: String) {
            openedSystemPath = systemPath
            openedUserPath = userPath
        }
        override fun systemSnapshot(): String = if (invalidSnapshot) "[1]" else JsonObject(entries).toString()
        override fun userSnapshot(): String = JsonObject(userEntries).toString()
        override fun set(isUserPref: Boolean, key: String, valueJson: String) {
            if (failWrites) throw IllegalStateException("write failed")
            (if (isUserPref) userEntries else entries)[key] = Json.parseToJsonElement(valueJson)
            writes++
        }
        override fun remove(isUserPref: Boolean, key: String) {
            (if (isUserPref) userEntries else entries).remove(key)
        }
    }
}
