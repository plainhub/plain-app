package com.ismartcoding.plain.features.media.actions

import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MediaActionRequestTest {
    @Test
    fun mutationsUseTheSelectedClientAndSkipEmptySelections() = runTest {
        val requests = mutableListOf<GraphqlOperation>()
        val client = MediaActionApi(GraphqlClient {
            requests.add(it)
            GraphqlResponse(data = JsonHelper.jsonDecode("""{"trashMediaItems":{"affectedCount":2}}"""))
        })
        assertEquals(2, client.run(DataType.IMAGE, MediaAction.TRASH, listOf("target-one", "target-two")))
        assertEquals(JsonPrimitive("ids:=target-one,target-two trash:false"), requests.single().variables["query"])
        assertEquals(JsonPrimitive("IMAGE"), requests.single().variables["type"])
        assertEquals(0, client.run(DataType.IMAGE, MediaAction.TRASH, emptyList()))
        assertEquals(1, requests.size)
    }

    @Test
    fun targetMutationFailurePropagatesWithoutANativeFallback() = runTest {
        var calls = 0
        val client = MediaActionApi(GraphqlClient {
            calls++
            error("target unavailable")
        })
        assertEquals("target unavailable", assertFailsWith<IllegalStateException> {
            client.run(DataType.AUDIO, MediaAction.DELETE, listOf("target-audio"))
        }.message)
        assertEquals(1, calls)
    }

    @Test
    fun usesExistingMediaOperations() {
        assertEquals("trashMediaItems", Mutation.mediaAction(MediaAction.TRASH, DataType.IMAGE, "ids:=1", "").field)
        assertEquals("restoreMediaItems", Mutation.mediaAction(MediaAction.RESTORE, DataType.IMAGE, "ids:=1", "").field)
        assertEquals("deleteMediaItems", Mutation.mediaAction(MediaAction.DELETE, DataType.IMAGE, "ids:=1", "").field)
        assertEquals("moveMediaItems", Mutation.mediaAction(MediaAction.MOVE, DataType.IMAGE, "ids:=1", "").field)
    }

    @Test
    fun selectionUsesOnlyDistinctRequestedIdsAndExplicitTrashState() {
        assertEquals("ids:=17,volume:42 trash:false", mediaActionSelection(listOf("17", "volume:42", "17"), false))
        assertEquals("ids:=17 trash:true", mediaActionSelection(listOf("17"), true))
    }

    @Test
    fun rejectsEmptyAndAmbiguousCsvSelections() {
        assertFailsWith<IllegalArgumentException> { mediaActionSelection(emptyList(), false) }
        for (id in listOf("", "17,18")) {
            assertFailsWith<IllegalArgumentException>(id) { mediaActionSelection(listOf(id), false) }
        }
    }

    @Test
    fun opaqueIdsAreEscapedAsLiteralDslValues() {
        assertEquals("ids:==opaque trash:false", mediaActionSelection(listOf("=opaque"), false))
        assertEquals("ids:=17\\ all:true trash:false", mediaActionSelection(listOf("17 all:true"), false))
        assertEquals("ids:=\\\"quoted\\\"\\\\path trash:false", mediaActionSelection(listOf("\"quoted\"\\path"), false))
        assertEquals("ids:=媒体：42 trash:false", mediaActionSelection(listOf("媒体：42"), false))
        assertEquals("ids:=17\\\nall:true trash:false", mediaActionSelection(listOf("17\nall:true"), false))
    }
}
