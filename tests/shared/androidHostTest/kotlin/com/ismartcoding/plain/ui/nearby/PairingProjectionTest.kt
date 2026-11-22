package com.ismartcoding.plain.ui.nearby

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.features.nearby.PairingMode
import com.ismartcoding.plain.models.PairingErrorCode
import com.ismartcoding.plain.models.PairingProgressNotice
import com.ismartcoding.plain.models.PairingRequestNotice
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.rust.graphql.GraphqlError
import com.ismartcoding.plain.rust.graphql.GraphqlException
import com.ismartcoding.plain.ui.app.MainViewModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PairingProjectionTest {
    @AfterTest
    fun clear() {
        NearbyViewModel.pairingViews.clear()
        NearbyViewModel.itemStatus.clear()
        MainViewModel.pendingPairingRequest.value = null
    }
    @Test
    fun failuresSelectPairingMessagesThroughTheNotificationEntryPoint() {
        val messages = listOf(
            PairingErrorCode.TIMEOUT to Res.string.pairing_expired,
            PairingErrorCode.REJECTED to Res.string.pairing_rejected,
            PairingErrorCode.TOO_MANY_LOGIN_ATTEMPTS to Res.string.pairing_too_many_attempts,
            PairingErrorCode.INVALID_PASSWORD to Res.string.invalid_password,
            PairingErrorCode.PEER_IDENTITY_CHANGED to Res.string.pairing_manage_identity_changed,
            PairingErrorCode.INTERNAL_ERROR to Res.string.pairing_failed,
        )
        for ((reason, message) in messages) {
            PairingProjection.starting("peer")
            PairingProjection.failed(PairingProgressNotice("peer", "Fixture", reason))
            assertEquals(message, NearbyViewModel.pairingViews.getValue("peer").error.messageResource())
        }
    }

    @Test
    fun failedPairingCommandUsesTheServerErrorCodeRatherThanItsRawMessage() {
        val notice = PairingProgressNotice("peer", "Fixture")
        for ((wire, expected) in listOf(
            "REJECTED" to Res.string.pairing_rejected,
            "UNKNOWN_CODE" to Res.string.pairing_failed,
        )) {
            PairingProjection.starting(notice.deviceId)
            val failure = GraphqlException(listOf(GraphqlError("unlocalized server text",
                extensions = JsonObject(mapOf("code" to JsonPrimitive(wire))))))
            PairingProjection.failed(notice, failure)
            val view = NearbyViewModel.pairingViews.getValue(notice.deviceId)
            assertEquals(PairingStep.FAILED, view.step)
            assertEquals(expected, view.error.messageResource())
            assertEquals(null, NearbyViewModel.itemStatus[notice.deviceId])
        }
    }

    @Test
    fun notifications_drive_local_display_and_release_waiting_actions() {
        PairingProjection.starting("peer")
        assertEquals(NearbyItemStatus.STARTING, NearbyViewModel.itemStatus["peer"])
        PairingProjection.started(PairingProgressNotice("peer", "Fixture", verificationCode = "01234567"))
        assertEquals("01234567", NearbyViewModel.pairingViews["peer"]?.verificationCode)
        assertEquals(NearbyItemStatus.PAIRING, NearbyViewModel.itemStatus["peer"])
        PairingProjection.failed(PairingProgressNotice("peer", "Fixture", PairingErrorCode.PASSWORD_REQUIRED))
        assertEquals(PairingStep.PASSWORD_REQUIRED, NearbyViewModel.pairingViews["peer"]?.step)
        assertEquals(null, NearbyViewModel.itemStatus["peer"])
        PairingProjection.starting("peer")
        PairingProjection.canceled(PairingProgressNotice("peer", "Fixture"))
        assertEquals(PairingStep.CANCELED, NearbyViewModel.pairingViews["peer"]?.step)
        assertEquals(null, NearbyViewModel.itemStatus["peer"])
        PairingProjection.starting("peer")
        PairingProjection.completed(PairingProgressNotice("peer", "Fixture"))
        assertEquals(NearbyItemStatus.PAIRED, NearbyViewModel.itemStatus["peer"])
    }
    @Test
    fun timeoutLeavesAnExpiredTerminalResultWithoutTreatingRejectionAsExpiry() {
        PairingProjection.starting("peer")
        PairingProjection.failed(PairingProgressNotice("peer", "Fixture", PairingErrorCode.TIMEOUT))
        val expired = NearbyViewModel.pairingViews.getValue("peer")
        assertEquals(true, expired.expired)
        assertEquals(false, expired.active)
        assertEquals(null, NearbyViewModel.itemStatus["peer"])

        PairingProjection.starting("peer")
        PairingProjection.failed(PairingProgressNotice("peer", "Fixture", PairingErrorCode.REJECTED))
        assertEquals(false, NearbyViewModel.pairingViews.getValue("peer").expired)
    }

    @Test
    fun a_confirmation_notice_displays_the_verified_peer_code() {
        PairingProjection.starting("peer")
        PairingProjection.request(PairingRequestNotice("peer", "Fixture", "", DeviceType.PHONE, "01234567", true))
        val view = NearbyViewModel.pairingViews.getValue("peer")
        assertEquals(PairingStep.TRUST_REQUIRED, view.step)
        assertEquals("01234567", view.verificationCode)
        assertEquals(true, view.identityChanged)
    }

    @Test
    fun repeatedOutboundTrustNoticeDoesNotNavigateToTheIncomingRequestPage() {
        val notice = PairingRequestNotice("peer", "Fixture", "", DeviceType.PHONE, "01234567", true)
        PairingProjection.starting("peer")
        PairingProjection.request(notice)
        PairingProjection.request(notice)
        assertEquals(PairingStep.TRUST_REQUIRED, NearbyViewModel.pairingViews["peer"]?.step)
        assertEquals(null, MainViewModel.pendingPairingRequest.value)
    }

    @Test
    fun lateProgressDoesNotResurrectATerminalAttempt() {
        PairingProjection.starting("peer")
        PairingProjection.canceled(PairingProgressNotice("peer", "Fixture"))
        PairingProjection.started(PairingProgressNotice("peer", "Fixture", verificationCode = "01234567"))
        assertEquals(PairingStep.CANCELED, NearbyViewModel.pairingViews["peer"]?.step)
        assertEquals(null, NearbyViewModel.itemStatus["peer"])
    }

    @Test
    fun manageTrustHasNoCodeUntilTheRemotePeerIsWaiting() {
        val localNotice = PairingRequestNotice("peer", "Fixture", "", DeviceType.PHONE, "01234567", true)
        val progress = PairingProgressNotice("peer", "Fixture", verificationCode = "98765432")
        PairingProjection.starting("peer")
        PairingProjection.request(localNotice)
        PairingProjection.started(progress)
        assertEquals(PairingStep.TRUST_REQUIRED, NearbyViewModel.pairingViews["peer"]?.step)
        assertEquals(null, NearbyViewModel.pairingViews["peer"]?.comparisonCode(PairingMode.MANAGE))
        assertEquals("01234567", NearbyViewModel.pairingViews["peer"]?.comparisonCode(PairingMode.CHAT))
        PairingProjection.trustAccepted("peer")
        PairingProjection.request(localNotice)
        assertEquals(null, MainViewModel.pendingPairingRequest.value)
        assertEquals(null, NearbyViewModel.pairingViews["peer"]?.comparisonCode(PairingMode.MANAGE))
        PairingProjection.started(progress)
        assertEquals("98765432", NearbyViewModel.pairingViews["peer"]?.comparisonCode(PairingMode.MANAGE))
    }

    @Test
    fun cancelClearsOnlyTheMatchingIncomingRequest() {
        val incoming = PairingRequestNotice("sender", "Fixture", "", DeviceType.PHONE, "01234567", false)
        PairingProjection.request(incoming)
        PairingProjection.canceled(PairingProgressNotice("other", "Other"))
        assertEquals(incoming, MainViewModel.pendingPairingRequest.value)
        PairingProjection.canceled(PairingProgressNotice("sender", "Fixture"))
        assertEquals(null, MainViewModel.pendingPairingRequest.value)
    }

    @Test
    fun aDifferentPeerStillGetsAnIncomingRequest() {
        val incoming = PairingRequestNotice("other", "Fixture", "", DeviceType.PHONE, "01234567", false)
        PairingProjection.starting("peer")
        PairingProjection.request(incoming)
        assertEquals(incoming, MainViewModel.pendingPairingRequest.value)
    }
}
