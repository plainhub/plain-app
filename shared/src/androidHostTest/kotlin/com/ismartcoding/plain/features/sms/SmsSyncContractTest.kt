package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.MmsSendResultData
import com.ismartcoding.plain.events.SendResultCodes
import com.ismartcoding.plain.events.SmsSendResultData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsSyncContractTest {
    @Test
    fun `SMS synchronization event IDs are additive and unique`() {
        assertEquals(35, EventType.SMS_PROVIDER_CHANGED.value)
        assertEquals(36, EventType.SMS_SEND_RESULT.value)
        assertEquals(37, EventType.MMS_SEND_RESULT.value)
        assertEquals(EventType.entries.size, EventType.entries.map { it.value }.toSet().size)
    }

    @Test
    fun `content MMS predicate includes sent and retrieved PDUs`() {
        assertEquals("m_type IN (128, 132)", SmsProviderContract.MMS_CONTENT_FILTER)
    }

    @Test
    fun `provider change bursts coalesce distinct URIs and clear after delivery`() {
        val changes = SmsProviderChangeBuffer()
        changes.start()

        assertEquals(true, changes.add("content://sms/1"))
        assertEquals(true, changes.add("content://mms/2"))
        assertEquals(true, changes.add("content://sms/1"))
        assertEquals(listOf("content://sms/1", "content://mms/2"), changes.drain())
        assertEquals(emptyList<String>(), changes.drain())
    }

    @Test
    fun `stopping provider changes clears queued work and rejects later changes`() {
        val changes = SmsProviderChangeBuffer()
        changes.start()
        changes.add("content://sms/1")

        changes.stop()

        assertEquals(emptyList<String>(), changes.drain())
        assertEquals(false, changes.add("content://mms/2"))
    }

    @Test
    fun `message IDs are partitioned between SMS and MMS providers`() {
        val ids = SmsProviderContract.partitionMessageIds("12,mms_34,56,mms_78")

        assertEquals(listOf("12", "56"), ids.sms)
        assertEquals(listOf("34", "78"), ids.mms)
    }

    @Test
    fun `all non-self group participants are preserved in provider order`() {
        val addresses = SmsProviderContract.selectConversationAddresses(
            addresses = listOf("+1 (602) 555-0100", "+1 480 555 0101", "+1 623 555 0102"),
            ownNumbers = setOf("6025550100"),
        )

        assertEquals(listOf("+1 480 555 0101", "+1 623 555 0102"), addresses)
    }

    @Test
    fun `participant filtering falls back when every resolved address is self`() {
        val addresses = SmsProviderContract.selectConversationAddresses(
            addresses = listOf("+1 602 555 0100"),
            ownNumbers = setOf("6025550100"),
        )

        assertEquals(listOf("+1 602 555 0100"), addresses)
    }

    @Test
    fun `recipient IDs retain every distinct participant`() {
        assertEquals(
            listOf("7", "8", "9"),
            SmsProviderContract.parseRecipientIds(" 7  8 9 8 "),
        )
    }

    @Test
    fun `multipart SMS emits success only after every part succeeds`() {
        val tracker = newSmsTracker()
        tracker.register("request", "client", "pending-a", 2, 100L)

        assertEquals(null, tracker.record("request", 0, 2, -1, -1, 200L))
        assertEquals(
            SmsSendResultData("pending-a", true, -1),
            tracker.record("request", 1, 2, -1, -1, 300L),
        )
    }

    @Test
    fun `multipart SMS emits one terminal failure`() {
        val tracker = newSmsTracker()
        tracker.register("request", "client", "pending-a", 2, 100L)

        assertEquals(
            SmsSendResultData("pending-a", false, 4),
            tracker.record("request", 0, 2, 4, -1, 200L),
        )
        assertEquals(null, tracker.record("request", 1, 2, -1, -1, 300L))
    }

    @Test
    fun `one client can correlate concurrent SMS sends independently`() {
        val tracker = newSmsTracker()
        tracker.register("send-a", "client", "pending-a", 1, 100L)
        tracker.register("send-b", "client", "pending-b", 1, 110L)

        assertEquals(
            SmsSendResultData("pending-b", false, 4),
            tracker.record("send-b", 0, 1, 4, -1, 200L),
        )
        assertEquals(
            SmsSendResultData("pending-a", true, -1),
            tracker.record("send-a", 0, 1, -1, -1, 210L),
        )
    }

    @Test
    fun `SMS send timeout removes pending correlation exactly once`() {
        val tracker = newSmsTracker()
        tracker.register("request", "client", "pending-a", 1, 100L)

        assertEquals(
            SmsSendResultData("pending-a", false, SmsProviderContract.SEND_RESULT_TIMEOUT),
            tracker.expire("request", 500L),
        )
        assertEquals(null, tracker.expire("request", 600L))
        tracker.acknowledge("request")
        assertEquals(emptyList<SmsPendingSendState>(), tracker.pending())
    }

    @Test
    fun `terminal SMS result remains replayable until bounded outbox cleanup`() {
        val store = FakeSmsSendStateStore()
        SmsSendStateTracker(store).apply {
            register("request", "client", "pending-a", 1, 100L)
            assertEquals(
                SmsSendResultData("pending-a", true, -1),
                record("request", 0, 1, -1, -1, 500L),
            )
        }

        val restored = SmsSendStateTracker(store)
        assertEquals(listOf(SmsSendResultData("pending-a", true, -1)), restored.terminalResults())
        assertEquals(listOf(SmsSendResultData("pending-a", true, -1)), restored.terminalResults())
        assertEquals(500L, restored.pending().single().terminalAtMillis)
        restored.acknowledge("request")
        assertTrue(restored.terminalResults().isEmpty())
    }

    @Test
    fun `multipart SMS state survives tracker recreation`() {
        val store = FakeSmsSendStateStore()
        SmsSendStateTracker(store).apply {
            register("request", "client", "pending-a", 2, 100L)
            assertEquals(null, record("request", 0, 2, -1, -1, 200L))
        }

        val restored = SmsSendStateTracker(store)
        assertEquals(
            SmsSendResultData("pending-a", true, -1),
            restored.record("request", 1, 2, -1, -1, 300L),
        )
        assertEquals(setOf(0, 1), restored.pending().single().completedParts)
        restored.acknowledge("request")
        assertTrue(restored.pending().isEmpty())
    }

    @Test
    fun `SMS PendingIntent identities are unique per request and part`() {
        val first = SmsProviderContract.smsSentIntentIdentity("com.example", "request-a", 0)
        val secondPart = SmsProviderContract.smsSentIntentIdentity("com.example", "request-a", 1)
        val secondRequest = SmsProviderContract.smsSentIntentIdentity("com.example", "request-b", 0)

        assertNotEquals(first, secondPart)
        assertNotEquals(first, secondRequest)
        assertTrue(first.action.contains("request-a.0"))
        assertTrue(first.data.endsWith("/request-a/0"))
    }

    @Test
    fun `phone normalization preserves country codes and normalizes punctuation`() {
        assertFalse(SmsProviderContract.addressesMatch("+44 20 1234 5678", "+1 20 1234 5678"))
        assertTrue(SmsProviderContract.addressesMatch("12-345", "12345"))
        assertTrue(SmsProviderContract.addressesMatch("Alice@Example.com", "alice@example.com"))
    }

    @Test
    fun `suffix phone matching requires unambiguous candidate context`() {
        val local = "6025550100"
        val us = "+1 602 555 0100"
        val uk = "+44 602 555 0100"

        assertTrue(SmsProviderContract.addressesMatch(us, local, listOf(us)))
        assertFalse(SmsProviderContract.addressesMatch(us, local, listOf(us, uk)))
    }

    @Test
    fun `MMS matching rejects incomplete body and attachment evidence`() {
        val requested = SmsProviderContract.MmsSendFingerprint("+16025550100", "hello", "7", listOf("image/jpeg"))
        val candidates = listOf(
            SmsProviderContract.MmsCandidateFingerprint(1, "+16025550100", "", "7", listOf("image/jpeg")),
            SmsProviderContract.MmsCandidateFingerprint(2, "+16025550100", "hello", "7", emptyList()),
            SmsProviderContract.MmsCandidateFingerprint(3, "+16025550100", "hello", "7", listOf("image/jpeg")),
        )

        assertEquals(listOf(3L), SmsProviderContract.matchingMmsCandidateIds(requested, candidates))
    }

    @Test
    fun `MMS matching uses normalized transformation tolerant attachment types`() {
        val requested = SmsProviderContract.MmsSendFingerprint(
            "6025550100",
            "same",
            "",
            listOf(" IMAGE/JPEG ", "text/x-vcard"),
        )
        val candidates = listOf(
            SmsProviderContract.MmsCandidateFingerprint(
                10,
                "+16025550100",
                "same",
                "8",
                listOf("text/x-vcard", "image/png"),
            ),
            SmsProviderContract.MmsCandidateFingerprint(
                11,
                "+16025550100",
                "same",
                "8",
                listOf("audio/mpeg", "text/x-vcard"),
            ),
        )

        assertEquals(listOf(10L), SmsProviderContract.matchingMmsCandidateIds(requested, candidates))
    }

    @Test
    fun `indistinguishable pending MMS sends are serialized`() {
        val first = SmsProviderContract.MmsSendFingerprint(
            "+1 602 555 0100",
            "same",
            "8",
            listOf("image/jpeg"),
        )
        val duplicate = SmsProviderContract.MmsSendFingerprint(
            "6025550100",
            "same",
            "8",
            listOf("IMAGE/JPEG"),
        )
        val transcoded = duplicate.copy(attachmentContentTypes = listOf("image/png"))
        val distinct = duplicate.copy(attachmentContentTypes = listOf("audio/mpeg"))

        assertTrue(SmsProviderContract.mmsOperationsAreIndistinguishable(first, duplicate))
        assertTrue(SmsProviderContract.mmsOperationsAreIndistinguishable(first, transcoded))
        assertFalse(SmsProviderContract.mmsOperationsAreIndistinguishable(first, distinct))
    }

    @Test
    fun `MMS serialization covers wildcard body and thread matcher domains`() {
        val wildcard = SmsProviderContract.MmsSendFingerprint(
            "6025550100",
            "",
            "",
            listOf("image/jpeg"),
        )
        val specific = wildcard.copy(body = "caption", threadId = "8")

        assertTrue(SmsProviderContract.mmsOperationsAreIndistinguishable(wildcard, specific))
        assertTrue(SmsProviderContract.mmsOperationsAreIndistinguishable(specific, wildcard))
    }

    @Test
    fun `MMS text matching spans parts and applies every filter`() {
        val parts = listOf("First line", "Second Line")

        assertTrue(SmsProviderContract.mmsTextMatches(parts, listOf("first", "LINE")))
        assertFalse(SmsProviderContract.mmsTextMatches(parts, listOf("first", "missing")))
    }

    @Test
    fun `MMS send timeout is a terminal correlated failure`() {
        assertEquals(
            MmsSendResultData("pending", true, 0),
            MmsSendResultData.success("pending"),
        )
        assertEquals(
            MmsSendResultData(
                pendingId = "pending",
                success = false,
                resultCode = SmsProviderContract.SEND_RESULT_TIMEOUT,
            ),
            MmsSendResultData.timeout("pending"),
        )
        assertEquals(
            MmsSendResultData("pending", false, SendResultCodes.CANCELLED),
            MmsSendResultData.cancelled("pending"),
        )
    }

    @Test
    fun `MMS terminal outbox survives recreation expires by terminal time and stays bounded`() {
        val store = FakeMmsSendResultStateStore()
        MmsSendResultOutbox(store, maxEntries = 2).apply {
            record(MmsSendResultData.success("old"), 100L)
            record(MmsSendResultData.timeout("middle"), 200L)
            record(MmsSendResultData.cancelled("new"), 300L)
        }

        val restored = MmsSendResultOutbox(store, maxEntries = 2)
        assertEquals(
            listOf(MmsSendResultData.timeout("middle"), MmsSendResultData.cancelled("new")),
            restored.replayable(nowMillis = 350L, ttlMillis = 200L),
        )
        assertEquals(
            listOf(MmsSendResultData.cancelled("new")),
            restored.replayable(nowMillis = 450L, ttlMillis = 200L),
        )
    }

    @Test
    fun `large MMS ID selections are numeric literal chunks without bind arguments`() {
        val predicate = SmsProviderContract.numericIdPredicate(
            "_id",
            (1..1001).map(Int::toString),
        ).orEmpty()

        assertEquals(3, Regex("_id IN").findAll(predicate).count())
        assertFalse(predicate.contains('?'))
        assertTrue(predicate.contains("1001"))
        assertEquals(null, SmsProviderContract.numericIdPredicate("_id", listOf("1", "not-an-id")))
    }

    private fun newSmsTracker() = SmsSendStateTracker(FakeSmsSendStateStore())

    private class FakeSmsSendStateStore : SmsSendStateStore {
        private val states = mutableMapOf<String, SmsPendingSendState>()

        override fun read(requestId: String): SmsPendingSendState? = states[requestId]

        override fun readAll(): List<SmsPendingSendState> = states.values.toList()

        override fun write(state: SmsPendingSendState) {
            states[state.requestId] = state
        }

        override fun remove(requestId: String) {
            states.remove(requestId)
        }
    }

    private class FakeMmsSendResultStateStore : MmsSendResultStateStore {
        private val states = mutableMapOf<String, MmsTerminalResultState>()

        override fun readAll(): List<MmsTerminalResultState> = states.values.toList()

        override fun write(state: MmsTerminalResultState) {
            states[state.pendingId] = state
        }

        override fun remove(pendingId: String) {
            states.remove(pendingId)
        }
    }
}
