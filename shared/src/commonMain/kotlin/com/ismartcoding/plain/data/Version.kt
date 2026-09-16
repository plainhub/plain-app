package com.ismartcoding.plain.data

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.db.IMedia
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.onlineClientIds
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import kotlin.time.Instant

class Version(numbers: List<String>) {

    private var major: Int = 0
    private var minor: Int = 0
    private var point: Int = 0

    init {
        major = numbers.getOrNull(0)?.toIntOrNull() ?: 0
        minor = numbers.getOrNull(1)?.toIntOrNull() ?: 0
        point = numbers.getOrNull(2)?.toIntOrNull() ?: 0
    }

    constructor() : this(listOf())
    constructor(string: String?) : this(string?.split(".") ?: listOf())

    override fun toString() = "$major.$minor.$point"

    /**
     * Use [major], [minor], [point] for comparison.
     *
     * 1. [major] <=> [other.major]
     * 2. [minor] <=> [other.minor]
     * 3. [point] <=> [other.point]
     */
    operator fun compareTo(other: Version): Int = when {
        major > other.major -> 1
        major < other.major -> -1
        minor > other.minor -> 1
        minor < other.minor -> -1
        point > other.point -> 1
        point < other.point -> -1
        else -> 0
    }

    fun whetherNeedUpdate(current: Version, skip: Version): Boolean = this > current && this > skip
}

fun String.toVersion(): Version = Version(this)


/**
 * Snapshot of all persistent service runtime statuses.
 */
data class ServiceDebugInfo(
    val httpServerRunning: Boolean = false,
    val httpServerState: String = "",
    val httpPort: Int = 0,
    val httpsPort: Int = 0,
    val wsSessionCount: Int = 0,
    val httpServerError: String = "",

    val mdnsRunning: Boolean = false,
    val mdnsHostname: String = "",

    val dlnaRunning: Boolean = false,
    val dlnaPlaybackState: String = "",
    val dlnaStartError: String = "",

    val bleRunning: Boolean = false,
    val bleClientId: String = "",
    val bleServiceUuid: String = "",

    val awareRunning: Boolean = false,
    val awareAttachStatus: String = "",
    val awareDiscoveredPeerCount: Int = 0,
)

