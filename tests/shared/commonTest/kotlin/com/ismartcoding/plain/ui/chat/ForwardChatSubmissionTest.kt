package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.features.chat.ChatApi
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import com.ismartcoding.plain.ui.chat.components.ForwardSubmission
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForwardChatSubmissionTest {
    @Test
    fun oneRequestCarriesTheOriginalRecipientsAndOptionalNote() = runTest {
        val requests = mutableListOf<GraphqlOperation>()
        val api = ChatApi(GraphqlClient("local fixture") {
            requests += it
            GraphqlResponse(data = buildJsonObject { put("forwardChatItems", JsonArray(emptyList())) })
        })
        val targets = listOf(ChatTarget.parseId("peer:simulator"), ChatTarget.parseId("channel:fixtures"))
        api.forward("original", targets, "note with \"quotes\"")
        val request = requests.single()
        assertTrue(request.query.contains($$"forwardChatItems(id: $id, targets: $targets, note: $note)"))
        assertEquals(JsonPrimitive("original"), request.variables["id"])
        assertEquals(JsonArray(targets.map { JsonPrimitive(it.encodedToId) }), request.variables["targets"])
        assertEquals(JsonPrimitive("note with \"quotes\""), request.variables["note"])
        api.forward("original", targets, " \n")
        assertEquals(JsonNull, requests.last().variables["note"])
    }

    @Test
    fun inFlightAndCommittedBatchesCannotBeSubmittedAgain() = runTest {
        val submission = ForwardSubmission()
        val began = CompletableDeferred<Unit>()
        val outcome = CompletableDeferred<Unit>()
        var calls = 0
        val attempt = async {
            submission.submit {
                calls++
                began.complete(Unit)
                outcome.await()
            }
        }
        began.await()
        assertTrue(submission.sending)
        assertFalse(submission.submit { calls++ })
        outcome.complete(Unit)
        assertTrue(attempt.await())
        assertFalse(submission.submit { calls++ })
        assertEquals(1, calls)
    }

    @Test
    fun anAmbiguousTransportResultDoesNotRetryTheBatch() = runTest {
        val submission = ForwardSubmission()
        assertFalse(submission.submit { error("Fixture response interrupted after commit") })
        assertTrue(submission.unconfirmed)
        assertFalse(submission.editable)
        var replayed = false
        assertFalse(submission.submit { replayed = true })
        assertFalse(replayed)
    }
}
