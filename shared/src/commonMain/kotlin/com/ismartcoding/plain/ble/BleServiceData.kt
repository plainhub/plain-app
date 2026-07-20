package com.ismartcoding.plain.ble

import com.ismartcoding.plain.crypto.sha256
import com.ismartcoding.plain.lib.extensions.toHexString

/**
 * Encodes/decodes the BLE scan-response `serviceData` payload.
 *
 * Format (9 bytes total, well within the 31-byte BLE advertising limit):
 *  - byte[0]     = aware flags bitfield (0x01 = Aware supported, 0x02 = Aware running)
 *  - byte[1..8]  = `SHA256(clientId)[0:8]` — an 8-byte truncated hash of the
 *                  full clientId (TempData.clientId, a 13-char short UUID).
 *
 * The full clientId is NOT broadcast — it is recovered later via the GATT
 * DISCOVER reply ([com.ismartcoding.plain.data.DDiscoverReply]). The 8-byte
 * shortId is only a stable per-peer match key: callers that know a peer's
 * full clientId can compute the same shortId via [shortIdOf] and compare.
 *
 * Total scan-response entry size:
 *  1 (length) + 1 (type) + 16 (128-bit service UUID) + 9 (payload) = 27 bytes.
 */
object BleServiceData {
    const val AWARE_SUPPORTED: Byte = 0x01
    const val AWARE_RUNNING: Byte = 0x02
    const val SHORT_ID_BYTES = 8
    const val PAYLOAD_BYTES = 1 + SHORT_ID_BYTES // flags + shortId

    /**
     * Returns the 8-byte truncated SHA256 of [clientId] as a lowercase hex
     * string (16 chars). This is the stable match key stored in
     * [com.ismartcoding.plain.ble.client.BleGattClient.id] and compared by
     * callers that already know the peer's full clientId (e.g. BleTransport,
     * PeerTransportPrewarmer).
     */
    fun shortIdOf(clientId: String): String {
        val hash = sha256(clientId.encodeToByteArray())
        return hash.copyOfRange(0, SHORT_ID_BYTES).toHexString()
    }

    /**
     * Builds the 9-byte serviceData payload for the BLE scan response.
     * Callers should pass the local device's current Aware state so peers can
     * read it without a GATT connection.
     */
    fun encode(awareSupported: Boolean, awareRunning: Boolean, clientId: String): ByteArray {
        val flags = buildAwareFlags(awareSupported, awareRunning)
        val shortIdBytes = sha256(clientId.encodeToByteArray()).copyOfRange(0, SHORT_ID_BYTES)
        return ByteArray(PAYLOAD_BYTES) { i ->
            if (i == 0) flags else shortIdBytes[i - 1]
        }
    }

    /**
     * Parses a serviceData byte array received from a BLE scan result.
     * Returns null when the payload is absent or too short.
     */
    fun decode(data: ByteArray?): Parts? {
        if (data == null || data.size < PAYLOAD_BYTES) return null
        val flags = data[0]
        val shortId = data.copyOfRange(1, 1 + SHORT_ID_BYTES).toHexString()
        return Parts(
            shortId = shortId,
            awareSupported = (flags.toInt() and AWARE_SUPPORTED.toInt()) != 0,
            awareRunning = (flags.toInt() and AWARE_RUNNING.toInt()) != 0,
        )
    }

    data class Parts(
        val shortId: String,
        val awareSupported: Boolean,
        val awareRunning: Boolean,
    )

    private fun buildAwareFlags(supported: Boolean, running: Boolean): Byte {
        var flags = 0
        if (supported) flags = flags or AWARE_SUPPORTED.toInt()
        if (running) flags = flags or AWARE_RUNNING.toInt()
        return flags.toByte()
    }
}
