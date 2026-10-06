package com.ismartcoding.plain.api

/**
 * Which platform host answers a method received on the Rust host socket.
 *
 * Kept separate from [RustHostApi] so the routing contract can be asserted on
 * the host JVM: a wrong prefix here makes one subsystem's method answer for
 * another's data, which only shows up as wrong payloads at runtime.
 */
enum class HostRoute {
    HttpExchange,
    Thumbnail,
    ChatPicked,
    SharedTransfer,
    BlePairing,
    MainGraphql,
    MdnsMulticast,
    DiscoveryAdvertisement,
    NearbyScan,
    PairingNotification,
    PeerGraphql,
    PeerTransport,
    SystemProvider,
    AudioEngine,
    FileTask,
    MediaAction,
    ImageIndex,
    AudioLibrary,
}

/**
 * Order matters: the first matching arm wins, so the broad `system` and
 * `audio` prefixes are checked after the specific ones they could swallow.
 */
fun routeForHostMethod(method: String): HostRoute = when {
    method == "httpExchange" -> HostRoute.HttpExchange
    method.startsWith("thumbnail") -> HostRoute.Thumbnail
    method.startsWith("chatPicked") -> HostRoute.ChatPicked
    method.startsWith("sharedTransfer") -> HostRoute.SharedTransfer
    method.startsWith("blePair") -> HostRoute.BlePairing
    method.startsWith("mainGraphql") -> HostRoute.MainGraphql
    method == "mdnsMulticast" -> HostRoute.MdnsMulticast
    method == "discoveryFacts" -> HostRoute.DiscoveryAdvertisement
    method == "nearbyScanFacts" -> HostRoute.NearbyScan
    method == "pairingNotification" -> HostRoute.PairingNotification
    method == "peerStartAware" || method == "peerDeviceInfo" -> HostRoute.PeerGraphql
    method.startsWith("peerTransport") -> HostRoute.PeerTransport
    method.startsWith("system") || method == "fileMetadataFacts" -> HostRoute.SystemProvider
    method == "audioEngineCommand" -> HostRoute.AudioEngine
    method.startsWith("fileTask") -> HostRoute.FileTask
    method == "mediaAction" -> HostRoute.MediaAction
    method.startsWith("imageIndex") -> HostRoute.ImageIndex
    else -> HostRoute.AudioLibrary
}

/** Method names the Rust side actually sends, used to prove no route is dead. */
internal val ALL_METHOD_NAMES = listOf(
    "httpExchange", "thumbnailDecode", "chatPickedFacts", "sharedTransferSend",
    "blePairConnect", "mainGraphql", "mdnsMulticast", "discoveryFacts",
    "nearbyScanFacts", "pairingNotification", "peerStartAware", "peerTransportSocket",
    "systemAppFacts", "fileMetadataFacts", "audioEngineCommand", "fileTaskScan",
    "mediaAction", "imageIndexStatus", "audioMetadata",
)
