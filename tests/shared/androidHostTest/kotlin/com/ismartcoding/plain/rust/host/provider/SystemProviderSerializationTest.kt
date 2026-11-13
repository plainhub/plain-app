package com.ismartcoding.plain.rust.host.provider

import com.ismartcoding.plain.rust.host.files.FileFacts

import com.ismartcoding.plain.features.telephony.sms.SmsConversationFacts
import com.ismartcoding.plain.features.telephony.sms.SmsCountFacts
import com.ismartcoding.plain.features.telephony.sms.SmsRowsFacts
import com.ismartcoding.plain.models.ScreenMirrorVideoCodec
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.lib.JsonHelper
import java.io.File
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.ismartcoding.plain.features.telephony.sms.MmsCandidateFacts
import com.ismartcoding.plain.features.telephony.sms.MmsPartFacts
import kotlinx.serialization.json.JsonElement

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
    fun contactProviderCarriesRawPageRowsAndColumns() {
        val facts = ContactFacts("1", "", "Given", "", "Family", "", "/photo", "local", true, "2", "/thumb", "", "2026-10-08T00:00:00Z")
        val page = ContactPageFacts(listOf(facts), listOf(ContactDataFacts("1", "vnd.android.cursor.item/im", "value", "", "", "", "6", "custom")), listOf(ContactGroupFacts("3", "Friends")))
        val encoded = JsonHelper.jsonEncode(page)
        assertEquals(page, JsonHelper.jsonDecode<ContactPageFacts>(encoded))
        assertTrue(encoded.contains("\"data5\":\"6\""))
        assertFalse(encoded.contains("phoneNumbers"))
        assertFalse(encoded.contains("organization"))
    }

    @Test
    fun nestedCodecKeepsNullFields() {
        assertEquals("""{"running":true,"controlEnabled":false,"codec":{"annexB":"abc","keyFrame":null}}""",
            JsonHelper.jsonEncode(ScreenMirrorStateFacts(true, false, ScreenMirrorVideoCodec("abc", null))))

    }

    @Test
    fun smsEmptyResponsesKeepPlatformContract() {
        assertEquals("""{"sms":0,"mms":0}""", JsonHelper.jsonEncode(SmsCountFacts(0, 0)))
        assertEquals("""{"items":[],"canonicalAddresses":[],"ownNumbers":[]}""", JsonHelper.jsonEncode(SmsRowsFacts(emptyList(), emptyList(), emptyList())))
        assertEquals("""{"items":[],"ownNumbers":[],"snippetCandidates":{}}""", JsonHelper.jsonEncode(SmsConversationFacts(emptyList(), emptyList(), emptyMap())))
    }

    @Test
    fun mmsFactsExposeRawPartsAndDatedCandidatesWithoutBusinessProjection() {
        val part = MmsPartFacts(
            "content://mms/part/1", "image/jpeg", "", "photo.jpg", true, "",
        )
        assertEquals(
            """{"path":"content://mms/part/1","contentType":"image/jpeg","name":"","fileName":"photo.jpg","hasData":true,"text":""}""",
            JsonHelper.jsonEncode(part),
        )
        val candidate = MmsCandidateFacts(
            1L, emptyList(), listOf(part),
        )
        val json = JsonHelper.jsonEncode(candidate)
        assertTrue(json.contains("\"parts\":"))
        assertTrue(!json.contains("\"body\":"))
        assertTrue(!json.contains("attachmentContentTypes"))
    }

    @Test
    fun elementDecodingRequiresTrackFieldsAndPreservesDefaults() {
        val input = JsonHelper.jsonDecode<JsonElement>(
            """{"title":"Song","artist":"Singer","path":"/song","durationMs":42,"extra":true}""",
        )
        assertEquals(AudioItem(title = "Song", path = "/song", artist = "Singer", durationMs = 42), JsonHelper.jsonDecodeFromElement<AudioItem>(input))
        assertThrows(SerializationException::class.java) {
            JsonHelper.jsonDecodeFromElement<AudioItem>(JsonHelper.jsonDecode("""{"title":"Song"}"""))
        }
    }

    @Test
    fun systemProviderSerializationUsesOnlyJsonHelper() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "shared/src/commonMain").isDirectory }
        val systemSources = File(root, "shared/src/commonMain/kotlin/com/ismartcoding/plain/rust/host")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name.startsWith("System") }
            .map { it.relativeTo(root).path }.toList()
        assertTrue(systemSources.isNotEmpty())
        val sources = systemSources + listOf(
            "shared/src/androidMain/kotlin/com/ismartcoding/plain/rust/host/RustHostApi.android.kt",
            "shared/src/androidMain/kotlin/com/ismartcoding/plain/features/telephony/sms/SmsHelper.kt",
            "shared/src/androidMain/kotlin/com/ismartcoding/plain/features/telephony/sms/SmsConversationHelper.kt",
            "shared/src/iosMain/kotlin/com/ismartcoding/plain/rust/host/RustHostApi.ios.kt",
        )
        val manualJson = Regex("""\b(buildJsonObject|buildJsonArray|JSONObject|JSONArray|JsonObject|JsonArray|JsonPrimitive|parseToJsonElement)\s*[(\{]""")
        for (path in sources) {
            assertEquals("Manual JSON construction in $path", emptyList<String>(),
                manualJson.findAll(File(root, path).readText()).map { it.value }.toList())
        }
    }
}
