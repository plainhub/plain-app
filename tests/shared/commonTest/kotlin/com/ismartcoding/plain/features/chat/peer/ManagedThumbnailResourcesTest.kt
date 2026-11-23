package com.ismartcoding.plain.features.chat.peer

import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.MediaBucketCover
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.platform.system.imageCacheKey
import com.ismartcoding.plain.platform.system.thumbnailDiskCacheKey
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class ManagedThumbnailResourcesTest {
    private val version = Instant.fromEpochMilliseconds(10)
    private fun covers(vararg paths: String) = paths.map { MediaBucketCover(it, version, null) }

    @Test
    fun folder_covers_share_a_peer_check_and_reuse_file_ids() = runTest {
        val fixture = Fixture()
        val client = fixture.client()
        val photo = "/fixtures/cover.jpg"
        val video = "/fixtures/cover.mov"
        val covers = client.folderThumbnailResources(covers(photo, video, photo), ResourceSize.XS)

        assertEquals(listOf(photo, video), covers.keys.toList())
        assertEquals("https://media.test:8443/fs?id=reference-1&action=PREVIEW&size=XS&v=1970-01-01T00%3A00%3A00.010Z", covers.getValue(photo).url)
        assertEquals("https://media.test:8443/fs?id=reference-2&action=PREVIEW&size=XS&v=1970-01-01T00%3A00%3A00.010Z", covers.getValue(video).url)
        assertEquals(1, fixture.peerChecks)
        assertEquals(2, fixture.encryptions)
        assertEquals(covers, client.folderThumbnailResources(covers(photo, video), ResourceSize.XS))
        assertEquals(2, fixture.peerChecks)
        assertEquals(2, fixture.encryptions)

        fixture.peer = fixture.peer.copy(token = "revoked-pairing")
        assertFailsWith<IllegalStateException> { client.folderThumbnailResources(covers(video), ResourceSize.XS) }
        assertEquals(2, fixture.encryptions)
    }

    @Test
    fun thumbnail_disk_identity_survives_client_reload_and_changes_with_version_or_authority() = runTest {
        val fixture = Fixture()
        val photo = "/fixtures/cover.jpg"
        val originalClient = fixture.client()
        val first = originalClient.folderThumbnailResources(covers(photo), ResourceSize.XS).getValue(photo)
        val firstKey = assertNotNull(first.thumbnailDiskCacheKey())
        assertEquals(firstKey, first.imageCacheKey())
        assertNull(originalClient.resource(photo).thumbnailDiskCacheKey())

        fixture.peer = fixture.peer.copy(ip = "new-address.test")
        val reloaded = fixture.client().folderThumbnailResources(covers(photo), ResourceSize.XS).getValue(photo)
        assertNotEquals(first.url, reloaded.url)
        assertEquals(firstKey, reloaded.thumbnailDiskCacheKey())

        val firstVersion = originalClient.resource(photo, ResourceSize.SM, version = Instant.fromEpochMilliseconds(1))
        val laterVersion = originalClient.resource(photo, ResourceSize.SM, version = Instant.fromEpochMilliseconds(2))
        assertEquals(firstVersion.thumbnailDiskCacheKey(), firstVersion.imageCacheKey())
        assertNotEquals(firstVersion.imageCacheKey(), laterVersion.imageCacheKey())
        assertNotEquals(firstKey, first.copy(tlsCertificateSha256 = "new-certificate").thumbnailDiskCacheKey())

        fixture.urlToken = "rotated-resource-token"
        originalClient.info()
        assertNotEquals(firstKey, originalClient.folderThumbnailResources(covers(photo), ResourceSize.XS).getValue(photo).thumbnailDiskCacheKey())
    }

    private class Fixture {
        var peer = Peer(
            id = "folder-cover-peer", name = "Media", ip = "media.test", port = 8443, status = PeerStatus.PAIRED,
            deviceType = DeviceType.COMPUTER, publicKey = "fixture-key", token = "fixture-pairing-token",
            createdAt = Instant.fromEpochMilliseconds(0), updatedAt = Instant.fromEpochMilliseconds(0),
        )
        var peerChecks = 0
        var encryptions = 0
        var urlToken = "resource-token"

        suspend fun client(): ManagedDeviceClient {
            val http = PeerHttpClient(
                peer,
                peers = { peerChecks++; listOf(peer) },
                clientId = { "fixture-client" },
                encryptResourceReference = { _, _, _ -> "reference-${++encryptions}" },
                executeGraphql = { _, _ ->
                    JsonHelper.jsonDecode("""{"data":{"app":{"deviceName":"Media","capabilities":[],"access":["VIDEOS"],"appDir":"app://","downloadsDir":"/downloads","urlToken":"$urlToken","tlsCertificateSha256":"fixture-certificate"}}}""")
                },
            )
            return ManagedDeviceClient.load(peer, http).also { peerChecks = 0 }
        }
    }
}
