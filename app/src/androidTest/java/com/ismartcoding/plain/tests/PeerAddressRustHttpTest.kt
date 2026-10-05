package com.ismartcoding.plain.tests

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getBestIp
import com.ismartcoding.plain.db.getName
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PeerAddressRustHttpTest {
    @Test
    fun rootAddressAndEncodedFileUrlUseCurrentPeerAndRejectStaleRows() = runBlocking {
        val id = "synthetic-address-${UUID.randomUUID()}"
        try {
            RustPeerStore.insert(DPeer(id = id, name = " ", ip = "bad, 127.0.0.1", port = 443))
            val peer = RustPeerStore.getById(id)!!
            assertEquals("bad, 127.0.0.1", peer.ip)
            assertEquals("127.0.0.1", peer.getBestIp())
            assertEquals("127.0.0.1", peer.getName())
            assertEquals("https://127.0.0.1/peer_graphql", RustPeerStore.address(peer).apiUrl)
            PeerCacher.load()
            assertEquals(peer.address, PeerCacher.getPeer(id)!!.address)
            val fileId = "id +/?#&中文%"
            val uri = Uri.parse(RustPeerStore.fileUrl(peer, fileId))
            assertEquals("127.0.0.1", uri.host)
            assertEquals("/fs", uri.path)
            assertEquals(fileId, uri.getQueryParameter("id"))
            RustPeerStore.update(peer.copy(ip = "::1", port = 2443))
            val current = RustPeerStore.getById(id)!!
            assertEquals("https://[::1]:2443/peer_graphql", current.address!!.apiUrl)
            assertEquals("wss://[::1]:2443/status", current.address!!.statusWsUrl)
            var rejected = false
            try { RustPeerStore.fileUrl(peer, fileId) } catch (_: Exception) { rejected = true }
            assertTrue(rejected)
            rejected = false
            try { RustPeerStore.address(peer) } catch (_: Exception) { rejected = true }
            assertTrue(rejected)
        } finally {
            RustPeerStore.delete(id)
            PeerCacher.load()
        }
    }
}
