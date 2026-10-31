package com.ismartcoding.plain.api.graphql

import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.audio.AudioSelectionAction
import com.ismartcoding.plain.features.dlna.sender.CastCommand
import com.ismartcoding.plain.features.dlna.sender.CastItem
import com.ismartcoding.plain.features.files.FileTaskOp
import com.ismartcoding.plain.features.files.FileTaskType
import com.ismartcoding.plain.features.media.actions.MediaAction
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.models.ImageEditorProject
import kotlin.time.Instant
import kotlinx.serialization.json.*
import kotlin.test.*

class GraphqlArgumentsTest {
    @Test
    fun mdns_status_only_requests_debug_data_when_selected() {
        val status = Query.mdnsState().operation.query
        val debug = Query.mdnsState(debug = true).operation.query
        assertTrue(status.contains("runtimeId revision running scanning"))
        for (field in listOf("services", "packetsIn", "packetsOut")) {
            assertFalse(status.contains(field))
            assertTrue(debug.contains(field))
        }
    }

    @Test
    fun typedRequestsOwnTheirResponseFieldsWithoutChangingTheEnvelope() {
        val requests = listOf(
            Query.audioPlaylistCovers(listOf("playlist")) to "audioPlaylistCovers",
            Query.audioPlaylists() to "audioPlaylists",
            Query.audioPlaylistItems("playlist", 0, 10, "") to "audioPlaylistItems",
            Query.audioPlayback() to "audioPlayback",
            Query.audioQueueIds(10) to "audioQueueItems",
            Query.audioQueueItemCount() to "audioQueueItemCount",
            Query.audioQueueItems(0, 10, "") to "audioQueueItems",
            Query.audioOverview() to "audioOverview",
            Mutation.createAudioPlaylist("Playlist") to "createAudioPlaylist",
            Mutation.advanceAudioSelection(AudioSelectionAction.NEXT, "", 0L, false) to "advanceAudioSelection",
            Query.contacts("", 10, 0) to "contacts",
            Query.calls("", 10, 0) to "calls",
            Query.sms("", 0, 10) to "sms",
            Query.packages("", 0, 10, FileSortBy.NAME_ASC) to "packages",
            Query.appFileName("file") to "appFiles",
            Query.feed("feed") to "feed",
            Query.feeds(false) to "feeds",
            Query.notes("", 10, 0) to "notes",
            Query.clipboardItems(10, 0, "") to "clipboardItems",
            Query.favoriteFolders() to "favoriteFolders",
            Query.shareRecords() to "shareRecords",
            Query.mediaRows(DataType.IMAGE, "", 10, 0, FileSortBy.NAME_ASC) to "images",
            Query.mediaCount(DataType.IMAGE, "") to "imageCount",
            Query.selectedMediaPaths("videos", "ids:=video", 1) to "videos",
            Query.mdnsState() to "mdnsState",
            Query.pairingStates() to "pairingStates",
            Mutation.saveNote("", "Title", "Content") to "createNote",
            Mutation.saveNote("note", "Title", "Content") to "updateNote",
        )

        for ((request, expected) in requests) {
            assertEquals(expected, request.field)
            val root = request.operation.query.substringAfter('{').trimStart().substringBefore('(').substringBefore(' ')
            assertEquals(expected, root)
            assertFalse("field" in JsonHelper.jsonDecode<JsonObject>(JsonHelper.jsonEncode(request.operation)))
        }
    }

    @Test
    fun typed_data_types_and_sort_orders_keep_graphql_enum_names() {
        assertEquals(JsonPrimitive("IMAGE"), Query.tags(DataType.IMAGE).operation.variables["type"])
        assertEquals(JsonPrimitive("IMAGE"), Mutation.createTag(DataType.IMAGE, "Tag").operation.variables["type"])
        assertEquals(
            JsonPrimitive("IMAGE"),
            Mutation.mediaAction(MediaAction.TRASH, DataType.IMAGE, "ids:=image", "").operation.variables["type"],
        )
        assertEquals(
            JsonPrimitive("NAME_ASC"),
            Query.mediaRows(DataType.IMAGE, "", 10, 0, FileSortBy.NAME_ASC).operation.variables["sortBy"],
        )
    }

    @Test
    fun valuesRemainSeparateFromTheDocumentAndRoundTripThroughTheEnvelope() {
        val input = "a\"b\\c\n) { deleteNotes(query: \"all:true\") }"
        val request = Query.chatCount(input)
        assertEquals(Query.chatCount("ordinary").operation.query, request.operation.query)
        assertFalse(request.operation.query.contains(input))
        assertTrue(request.operation.query.contains("chatCount(query: \$query)"))
        val envelope = JsonHelper.jsonDecode<JsonObject>(JsonHelper.jsonEncode(request.operation))
        assertEquals(input, envelope.getValue("variables").jsonObject.getValue("query").jsonPrimitive.content)
        assertEquals(request.operation.query, envelope.getValue("query").jsonPrimitive.content)
        val paths = listOf("a\"", "b\\c", "line\nbreak")
        assertEquals(paths, Mutation.deleteFiles(paths).operation.variables.getValue("paths").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun optionalUpdateArgumentsAreOmittedAndFalseIsRetained() {
        assertEquals(setOf("id"), Mutation.updateFeed("id", null, null, null).operation.variables.keys)
        val variables = Mutation.updateFeed("id", "", null, false).operation.variables
        assertEquals(JsonPrimitive(""), variables["name"])
        assertEquals(JsonPrimitive(false), variables["fetchContent"])
        assertFalse("url" in variables)
        assertEquals(JsonNull, Mutation.syncFeeds("").operation.variables["id"])
    }

    @Test
    fun nullAndEmptyRecipientsRemainDistinct() {
        assertEquals(JsonNull, Mutation.deliverChatItem("id", null).operation.variables["recipients"])
        assertEquals(JsonArray(emptyList()), Mutation.deliverChatItem("id", emptyList()).operation.variables["recipients"])
    }

    @Test
    fun batchAliasesAndVariablesStayUniqueWhenIdsRepeat() {
        val request = Query.chatSnapshot(listOf("a\"", "a\""))
        assertTrue(request.operation.query.contains("message0:chatItem(id: \$id0)"))
        assertTrue(request.operation.query.contains("message1:chatItem(id: \$id1)"))
        assertEquals(mapOf("id0" to JsonPrimitive("a\""), "id1" to JsonPrimitive("a\"")), request.operation.variables)
    }

    @Test
    fun onlyMoveIncludesDestination() {
        for (action in MediaAction.entries) {
            val request = Mutation.mediaAction(action, DataType.IMAGE, "ids:=1", "a\"b")
            assertEquals(action == MediaAction.MOVE, "destDir" in request.operation.variables)
            if (action == MediaAction.MOVE) assertEquals(JsonPrimitive("a\"b"), request.operation.variables["destDir"])
        }
    }

    @Test
    fun dlnaCommandsBindTypedInputsAndKeepResponseMetadataOffTheWire() {
        val item = CastItem("a\"b", "title", durationMs = 4_000, audio = true)
        val commands = listOf(
            CastCommand.StartScan to "startDlnaScan",
            CastCommand.StopScan to "stopDlnaScan",
            CastCommand.Select("id") to "selectDlnaDevice",
            CastCommand.Cast(item) to "castDlnaMedia",
            CastCommand.Play to "playDlna",
            CastCommand.Pause to "pauseDlna",
            CastCommand.Seek(42) to "seekDlna",
            CastCommand.Stop to "stopDlna",
            CastCommand.Exit to "exitDlna",
            CastCommand.Add(item) to "addDlnaItem",
            CastCommand.Remove("path") to "removeDlnaItem",
            CastCommand.RemoveAt(1) to "removeDlnaItemAt",
            CastCommand.Reorder(1, 2) to "reorderDlnaItems",
            CastCommand.Clear to "clearDlnaItems",
        )
        for ((command, expected) in commands) {
            val request = Mutation.dlna(command)
            assertEquals(expected, request.field)
            assertEquals(expected, request.operation.query.substringAfter('{').trimStart().substringBefore('(').substringBefore(' '))
            assertFalse("field" in JsonHelper.jsonDecode<JsonObject>(JsonHelper.jsonEncode(request.operation)))
        }
        val input = Mutation.dlna(CastCommand.Cast(item)).operation.variables.getValue("item").jsonObject
        assertEquals(JsonPrimitive(item.path), input["path"])
        assertEquals(JsonPrimitive(4_000L), input["durationMs"])
        assertEquals(JsonPrimitive(true), input["audio"])
    }

    @Test
    fun mediaSelectionsKeepTypeSpecificFields() {
        assertTrue(Query.mediaRows(DataType.AUDIO, "", 10, 0, FileSortBy.NAME_ASC).operation.query.contains("albumFileId"))
        assertTrue(Query.mediaRows(DataType.IMAGE, "", 10, 0, FileSortBy.NAME_ASC).operation.query.contains("takenAt"))
        assertFalse(Query.mediaRows(DataType.DOC, "", 10, 0, FileSortBy.NAME_ASC).operation.query.contains("isFavorite"))
        assertTrue(Query.mediaCount(DataType.VIDEO, "").operation.query.contains("videoCount(query: \$query)"))
    }

    @Test
    fun variableRequestsOwnNamesAndPreserveDefaultInputFields() {
        val path = "a\"b\\c\n"
        assertEquals(mapOf("path" to JsonPrimitive(path), "name" to JsonPrimitive("audio.mp3")), Query.audioDuration(path).operation.variables)
        assertEquals(mapOf("path" to JsonPrimitive(path)), Mutation.cacheAudioCover(path).operation.variables)
        val request = Mutation.createFileTask(FileTaskType.COPY, listOf(FileTaskOp("a", "b")))
        assertEquals(JsonPrimitive(false), request.operation.variables.getValue("ops").jsonArray.single().jsonObject["overwrite"])
        assertEquals(setOf("type", "title", "ops"), request.operation.variables.keys)
    }

    @Test
    fun structuredInputsExcludeOutputFieldsAndPreserveDatesAndNulls() {
        val created = Mutation.saveNote("", "title", "body")
        assertEquals("createNote", created.field)
        assertEquals(setOf("input"), created.operation.variables.keys)
        assertEquals(setOf("title", "content"), created.operation.variables.getValue("input").jsonObject.keys)
        val edited = Mutation.saveNote("id", "", "body")
        assertEquals("updateNote", edited.field)
        assertEquals(JsonPrimitive("id"), edited.operation.variables["id"])
        val project = Mutation.saveImageEditorProject("id", ImageEditorProject("id")).operation.variables.getValue("input").jsonObject
        assertEquals(setOf("stateB64", "thumbnail", "canvasWidth", "canvasHeight", "layerCount"), project.keys)
        assertEquals(JsonNull, project["thumbnail"])
        val expiresAt = Instant.parse("2026-01-01T00:00:00.123456789Z")
        assertEquals(JsonPrimitive(expiresAt.toString()), Mutation.createShare("share", listOf("path"), true, expiresAt).operation.variables["expiresAt"])
        assertEquals(JsonNull, Mutation.updateShare("id", "name", null, null).operation.variables["realPaths"])
        assertEquals(JsonNull, Query.shareLink("id", null).operation.variables["host"])
    }
}
