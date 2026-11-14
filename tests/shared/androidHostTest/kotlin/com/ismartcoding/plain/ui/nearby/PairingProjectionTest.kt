package com.ismartcoding.plain.ui.nearby

import com.ismartcoding.plain.models.PairingProgressNotice
import com.ismartcoding.plain.models.PairingRequestNotice
import com.ismartcoding.plain.platform.system.DeviceType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PairingProjectionTest {
    @AfterTest
    fun clear() {
        NearbyViewModel.pairingViews.clear()
        NearbyViewModel.itemStatus.clear()
    }
    @Test
    fun notifications_drive_local_display_and_release_waiting_actions() {
        PairingProjection.starting("peer")
        assertEquals(NearbyItemStatus.STARTING, NearbyViewModel.itemStatus["peer"])
        PairingProjection.started(PairingProgressNotice("peer", "Fixture", verificationCode = "01234567"))
        assertEquals("01234567", NearbyViewModel.pairingViews["peer"]?.verificationCode)
        assertEquals(NearbyItemStatus.PAIRING, NearbyViewModel.itemStatus["peer"])
        PairingProjection.failed(PairingProgressNotice("peer", "Fixture", "password_required"))
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
    fun a_confirmation_notice_displays_the_verified_peer_code() {
        PairingProjection.starting("peer")
        PairingProjection.request(PairingRequestNotice("peer", "Fixture", "", DeviceType.PHONE, "01234567", true))
        val view = NearbyViewModel.pairingViews.getValue("peer")
        assertEquals(PairingStep.TRUST_REQUIRED, view.step)
        assertEquals("01234567", view.verificationCode)
        assertEquals(true, view.identityChanged)
    }
}
