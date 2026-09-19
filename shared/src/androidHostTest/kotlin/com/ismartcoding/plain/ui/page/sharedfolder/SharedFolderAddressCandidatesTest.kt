package com.ismartcoding.plain.ui.page.sharedfolder

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DMessageShare
import com.ismartcoding.plain.db.DSharePeerInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the address candidate chain behind a share card: when the card is our
 * own share (peerInfo.id == clientId), today's local endpoints and loopback at
 * the current port lead, so an IP change since sending can't break the link.
 */
class SharedFolderAddressCandidatesTest {

    private fun msg(peerId: String, ip: String = "10.0.0.9", port: Int = 8443) = DMessageShare(
        shareId = "sid",
        urlToken = "tok",
        peerInfo = DSharePeerInfo(id = peerId, ip = ip, port = port),
        name = "folder",
    )

    private fun endpoints(links: List<com.ismartcoding.plain.features.share.SharedLink>) =
        links.map { "${it.host}:${it.port}" }

    @Test
    fun foreignCardUsesOnlyRecordedEndpoint() {
        assertEquals(
            listOf("10.0.0.9:8443"),
            endpoints(addressCandidates(msg("other-device"))),
            "a foreign share card keeps the message endpoint and the (empty) peer cache",
        )
    }

    @Test
    fun ownCardLeadsWithCurrentLocalEndpoints() {
        TempData.clientId = "self"
        TempData.httpsPort.value = 9999
        try {
            val candidates = addressCandidates(msg("self", ip = "10.0.0.9", port = 8443))
            assertEquals(
                9999,
                candidates.first().port,
                "self share must try the current local port before anything else",
            )
            assertTrue(
                candidates.any { it.host == "127.0.0.1" && it.port == 9999 },
                "loopback at the current port backs the interface candidates",
            )
            assertTrue(
                candidates.any { it.host == "10.0.0.9" && it.port == 8443 },
                "the recorded send-time endpoint stays as a fallback",
            )
        } finally {
            TempData.clientId = ""
            TempData.httpsPort.value = 8443
        }
    }

    @Test
    fun ownCardWithoutServerPortFallsBackToRecordedEndpoint() {
        TempData.clientId = "self"
        TempData.httpsPort.value = 0
        try {
            assertEquals(
                listOf("10.0.0.9:8443"),
                endpoints(addressCandidates(msg("self"))),
                "no local HTTP server bound means no local candidates",
            )
        } finally {
            TempData.clientId = ""
            TempData.httpsPort.value = 8443
        }
    }

    @Test
    fun sameHostPortCandidatesCollapseToOne() {
        TempData.clientId = "self"
        TempData.httpsPort.value = 8443
        try {
            val ip = com.ismartcoding.plain.platform.getDeviceIP4()
            if (ip.isEmpty()) return
            val candidates = addressCandidates(msg("self", ip = ip, port = 8443))
            assertEquals(
                1,
                candidates.count { "${it.host}:${it.port}" == "$ip:8443" },
                "a recorded endpoint equal to a current local one collapses into a single candidate",
            )
        } finally {
            TempData.clientId = ""
            TempData.httpsPort.value = 8443
        }
    }
}
