package com.ismartcoding.plain.platform.network.ble.client

import com.ismartcoding.plain.platform.network.ble.BleService
import com.ismartcoding.plain.models.Peer
import kotlinx.coroutines.flow.Flow

interface BleGattClient {
    val id: String
    val name: String?
    var rssi: Int

    fun isConnected(): Boolean

    suspend fun ensureConnected(retries: Int = 3): Boolean

    fun disconnect()

    val maximumWriteValueLength: Int

    suspend fun writeCharacteristic(service: BleService, value: ByteArray): Boolean

    suspend fun readCharacteristic(service: BleService): ByteArray?

    suspend fun setNotification(service: BleService, enable: Boolean): Boolean

    suspend fun waitForNotification(service: BleService, timeoutMs: Long): ByteArray?
}

interface BleScanner {
    fun scan(clientId: String? = null): Flow<BleGattClient>

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

    fun createClient(clientId: String): BleGattClient?

    fun isReadyToUse(): Boolean

    fun isAdvertiseReady(): Boolean

    suspend fun ensurePermission(): Boolean

    fun teardownConnection(device: BleGattClient)
}
