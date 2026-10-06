package com.ismartcoding.plain.api

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Rust host socket multiplexes every platform callback onto one JSON-RPC-ish
 * channel and dispatches by method-name prefix (see `RustHostApi.start`). The
 * routing table is the contract between Rust and Kotlin, so a silent
 * mis-route here surfaces as one host's method answering for another's data.
 */
class RustHostDispatchTest {

    private fun routeOf(method: String): HostRoute = routeForHostMethod(method)

    @Test
    fun explicitHostMethodsReachTheirOwnHost() {
        val expectations = mapOf(
            "httpExchange" to HostRoute.HttpExchange,
            "thumbnailDecode" to HostRoute.Thumbnail,
            "chatPickedFacts" to HostRoute.ChatPicked,
            "sharedTransferSend" to HostRoute.SharedTransfer,
            "blePairConnect" to HostRoute.BlePairing,
            "mainGraphql" to HostRoute.MainGraphql,
            "mdnsMulticast" to HostRoute.MdnsMulticast,
            "discoveryFacts" to HostRoute.DiscoveryAdvertisement,
            "nearbyScanFacts" to HostRoute.NearbyScan,
            "pairingNotification" to HostRoute.PairingNotification,
            "peerStartAware" to HostRoute.PeerGraphql,
            "peerDeviceInfo" to HostRoute.PeerGraphql,
            "peerTransportSocket" to HostRoute.PeerTransport,
            "systemAppFacts" to HostRoute.SystemProvider,
            "fileMetadataFacts" to HostRoute.SystemProvider,
            "audioEngineCommand" to HostRoute.AudioEngine,
            "fileTaskScan" to HostRoute.FileTask,
            "mediaAction" to HostRoute.MediaAction,
            "imageIndexStatus" to HostRoute.ImageIndex,
        )
        for ((method, expected) in expectations) {
            assertEquals("method '$method' must route to $expected", expected, routeOf(method))
        }
    }

    /**
     * The audio library owns the catch-all branch, so an unrecognised method
     * reaches `AudioLibraryHost.handle`, which rejects it with
     * "Unknown Rust host operation". Pin that no *known* host method can be
     * swallowed by it: every host that has its own `handle` must be reachable
     * by at least one method name.
     */
    @Test
    fun audioFamilyFallsBackToTheLibraryHost() {
        for (method in listOf(
            "audioMetadata",
            "audioLibraryCount",
            "audioLibraryPage",
            "audioLibraryLocate",
            "audioLibraryContains",
        )) {
            assertEquals(method, HostRoute.AudioLibrary, routeOf(method))
        }
    }

    @Test
    fun unknownMethodReachesTheAudioLibraryFallback() {
        assertEquals(HostRoute.AudioLibrary, routeOf("totallyUnknownMethod"))
        val failure = runCatching { error("Unknown Rust host operation: totallyUnknownMethod") }.exceptionOrNull()
        assertEquals(
            "Unknown Rust host operation: totallyUnknownMethod",
            failure?.message,
        )
    }

    /**
     * `system*` is a very broad prefix — the system provider answers for SMS,
     * calls, notifications, clips, packages and media probes. Pin the exact
     * families so a new subsystem cannot land there by accident.
     */
    @Test
    fun systemPrefixStaysWithTheSystemProvider() {
        for (method in listOf(
            "systemSmsFacts", "systemCallFacts", "systemNotificationFacts",
            "systemClipboardFacts", "systemPackageFacts", "systemAudioPlay",
            "systemContactFacts", "systemDeviceFacts",
        )) {
            assertEquals(method, HostRoute.SystemProvider, routeOf(method))
        }
    }

    @Test
    fun everyRouteIsReachable() {
        val reachable = ALL_METHOD_NAMES.map { routeOf(it) }.toSet()
        assertEquals(
            "a route declared in the dispatch table has no known method",
            HostRoute.entries.toSet(),
            reachable,
        )
    }
}
