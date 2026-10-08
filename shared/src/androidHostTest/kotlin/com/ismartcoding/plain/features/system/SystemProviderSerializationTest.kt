package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.sms.SmsConversationFacts
import com.ismartcoding.plain.features.sms.SmsCountFacts
import com.ismartcoding.plain.features.sms.SmsRowsFacts
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class SystemProviderSerializationTest {
    @Test
    fun fileFactsKeepEpochNumbersAndExplicitNulls() {
        val facts = FileFacts("a\"b", "/a", "", null, 1720000000000L, 5L, false, 0)
        assertEquals(
            """{"name":"a\"b","path":"/a","mediaId":"","createdAt":null,"updatedAt":1720000000000,"size":5,"isDir":false,"childCount":0}""",
            JsonHelper.jsonEncode(facts),
        )
    }

    @Test
    fun mediaFactsKeepStringTimestampsAndNullableTakenAt() {
        val facts = MediaFacts("1", "Photo", "/photo", 9, "2", "2026-10-08T00:00:00Z", "2026-10-08T00:00:00Z", 0, null, true)
        assertEquals(
            """{"id":"1","title":"Photo","path":"/photo","size":9,"bucketId":"2","createdAt":"2026-10-08T00:00:00Z","updatedAt":"2026-10-08T00:00:00Z","durationMs":0,"takenAt":null,"isFavorite":true,"width":0,"height":0,"rotation":0}""",
            JsonHelper.jsonEncode(facts),
        )
    }

    @Test
    fun contactFactsKeepPublicNamesAndRawDetailTypes() {
        val facts = ContactFacts(
            id = "1", prefix = "", firstName = "Ada", middleName = "", lastName = "Lovelace",
            suffix = "", nickname = "", photoId = "/photo",
            phoneNumbers = listOf(ContactPhoneFacts("123", 2, "mobile", "+123")),
            emails = listOf(ContactDetailFacts("ada@example.com", 1, "home")),
            addresses = emptyList(), events = emptyList(), websites = emptyList(), ims = emptyList(),
            source = "local", starred = true, contactId = "2", thumbnailId = "/thumb", notes = "",
            groups = listOf(ContactGroupFacts("3", "Friends")), organization = null,
            ringtone = "", updatedAt = "2026-10-08T00:00:00Z",
        )
        assertEquals(
            """{"id":"1","prefix":"","firstName":"Ada","middleName":"","lastName":"Lovelace","suffix":"","nickname":"","photoId":"/photo","phoneNumbers":[{"value":"123","type":2,"label":"mobile","normalizedNumber":"+123"}],"emails":[{"value":"ada@example.com","type":1,"label":"home"}],"addresses":[],"events":[],"websites":[],"ims":[],"source":"local","starred":true,"contactId":"2","thumbnailId":"/thumb","notes":"","groups":[{"id":"3","name":"Friends"}],"organization":null,"ringtone":"","updatedAt":"2026-10-08T00:00:00Z"}""",
            JsonHelper.jsonEncode(facts),
        )
    }

    @Test
    fun nestedCodecKeepsNullFields() {
        assertEquals("""{"running":true,"controlEnabled":false,"codec":{"annexB":"abc","keyFrame":null}}""",
            JsonHelper.jsonEncode(ScreenMirrorStateFacts(true, false, ScreenMirrorCodecFacts("abc", null))))

    }

    @Test
    fun smsEmptyResponsesKeepPlatformContract() {
        assertEquals("""{"sms":0,"mms":0}""", JsonHelper.jsonEncode(SmsCountFacts(0, 0)))
        assertEquals("""{"items":[],"canonicalAddress":""}""", JsonHelper.jsonEncode(SmsRowsFacts(emptyList(), "")))
        assertEquals("""{"items":[],"snippets":{}}""", JsonHelper.jsonEncode(SmsConversationFacts(emptyList(), emptyMap())))
    }

    @Test
    fun elementDecodingRequiresTrackFieldsAndPreservesDefaults() {
        val input = JsonHelper.jsonDecode<kotlinx.serialization.json.JsonElement>(
            """{"title":"Song","artist":"Singer","path":"/song","durationMs":42,"extra":true}""",
        )
        assertEquals(DPlaylistAudio("Song", "/song", "Singer", 42), JsonHelper.jsonDecodeFromElement<DPlaylistAudio>(input))
        assertThrows(SerializationException::class.java) {
            JsonHelper.jsonDecodeFromElement<DPlaylistAudio>(JsonHelper.jsonDecode("""{"title":"Song"}"""))
        }
    }

    @Test
    fun systemProviderSerializationUsesOnlyJsonHelper() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "shared/src/commonMain").isDirectory }
        val systemSources = File(root, "shared/src/commonMain/kotlin/com/ismartcoding/plain/features/system")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name.startsWith("System") }
            .map { it.relativeTo(root).path }.toList()
        val sources = systemSources + listOf(
            "shared/src/androidMain/kotlin/com/ismartcoding/plain/features/sms/SmsHelper.kt",
            "shared/src/androidMain/kotlin/com/ismartcoding/plain/features/sms/SmsConversationHelper.kt",
            "shared/src/iosMain/kotlin/com/ismartcoding/plain/platform/SystemProviders.ios.kt",
        )
        val manualJson = Regex("""\b(buildJsonObject|buildJsonArray|JSONObject|JSONArray|JsonObject|JsonArray|JsonPrimitive|parseToJsonElement)\s*[(\{]""")
        for (path in sources) {
            assertEquals("Manual JSON construction in $path", emptyList<String>(),
                manualJson.findAll(File(root, path).readText()).map { it.value }.toList())
        }
    }
}
