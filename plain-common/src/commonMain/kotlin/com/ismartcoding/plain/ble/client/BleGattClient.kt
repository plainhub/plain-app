package com.ismartcoding.plain.ble.client

import com.ismartcoding.plain.ble.BleService
import kotlinx.coroutines.flow.Flow

interface BleGattClient {
    // Stable peer match key parsed from the BLE scan response serviceData.
    // This is the 8-byte truncated SHA256 of the peer's full clientId
    // (TempData.clientId), rendered as a 16-char lowercase hex string — see
    // [com.ismartcoding.plain.ble.BleServiceData.shortIdOf]. The full clientId
    // is NOT broadcast; it is recovered later via the GATT DISCOVER reply.
    // Android's BLE MAC randomizes every ~15 minutes so it must NOT be used
    // as the peer id. The underlying BLE MAC (on Android) is exposed separately
    // via the platform-specific client.
    val id: String
    val name: String?
    var rssi: Int

    // Aware flags parsed from the BLE scan response serviceData (byte[0]).
    // These are a cheap pre-GATT hint — the authoritative values come from the
    // GATT DISCOVER reply ([com.ismartcoding.plain.discover.NearbyDeviceInfo]).
    val awareSupported: Boolean get() = false
    val awareRunning: Boolean get() = false

    fun isConnected(): Boolean

    suspend fun ensureConnected(retries: Int = 3): Boolean

    fun disconnect()

    suspend fun writeCharacteristic(service: BleService, value: String): Boolean

    suspend fun readCharacteristic(service: BleService): String?

    suspend fun setNotification(service: BleService, enable: Boolean): Boolean

    suspend fun waitForNotification(service: BleService, timeoutMs: Long): String?
}

interface BleScanner {
    fun scan(serviceUuid: String): Flow<BleGattClient>

    /**
     * Pauses an active scan without closing its flow. Scanning shares the
     * Bluetooth controller radio with GATT operations (connect, CCCD writes),
     * and concurrent scanning can starve those operations into timeouts.
     * Call before GATT-heavy sessions (e.g. pairing) and release with
     * [resumeScan] when they finish.
     *
     * Nestable: the radio restarts only when the last pause is released.
     */
    fun pauseScan()

    /**
     * Releases one [pauseScan]. Restarts scanning when the last pause is
     * released and the scan flow is still alive; no-op otherwise.
     */
    fun resumeScan()

    /**
     * Whether the scan radio is currently paused (at least one [pauseScan]
     * is unreleased). While paused, BLE devices cannot refresh their
     * last-seen time, so device-timeout cleanup must be skipped.
     */
    fun isScanPaused(): Boolean

    /**
     * Finds a discovered BLE device whose shortId matches [clientId]. The
     * shortId is computed via [com.ismartcoding.plain.ble.BleServiceData.shortIdOf]
     * (8-byte truncated SHA256 of the full clientId), so callers pass the
     * peer's full clientId (TempData.clientId / DPeer.id) and the scanner
     * matches it against the shortId broadcast in the scan response.
     */
    suspend fun findOne(clientId: String): BleGattClient?

    /**
     * Returns an already-discovered [BleGattClient] for [clientId], or null if
     * no matching device has been seen in the current scan session. Matching
     * is done by shortId (see [findOne]).
     *
     * On Android, BLE MACs can't be constructed from a clientId alone — the
     * underlying [android.bluetooth.BluetoothDevice] must come from a scan
     * result. Callers that need a client for a known clientId should use
     * [findOne] (which scans) or check the result of a prior [scan] flow.
     */
    fun createClient(clientId: String): BleGattClient?

    fun isReadyToUse(): Boolean

    fun isAdvertiseReady(): Boolean

    suspend fun ensurePermission(): Boolean

    fun teardownConnection(device: BleGattClient)
}
