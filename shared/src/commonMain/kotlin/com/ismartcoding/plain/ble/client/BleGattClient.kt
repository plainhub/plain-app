package com.ismartcoding.plain.ble.client

import com.ismartcoding.plain.ble.BleService
import kotlinx.coroutines.flow.Flow

interface BleGattClient {
    // The peer's clientId (TempData.clientId, a 13-char short UUID like
    // "t4a27f5gwnz8") parsed from the BLE scan response serviceData. This is
    // the stable peer identifier — Android's BLE MAC randomizes every ~15
    // minutes so it must NOT be used as the peer id. The underlying BLE MAC
    // (on Android) is exposed separately via the platform-specific client.
    val id: String
    val name: String?
    var rssi: Int
    // Bit flags parsed from the BLE scan response serviceData:
    //   bit 0 (0x01) = peer supports Wi-Fi Aware
    //   bit 1 (0x02) = peer's Wi-Fi Aware service is currently running
    // Refreshed by AndroidBleScanner from ScanResult.scanRecord.serviceData; defaults to 0 on iOS.
    var awareFlags: Int

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

    suspend fun findOne(id: String): BleGattClient?

    /**
     * Returns an already-discovered [BleGattClient] for [clientId], or null if
     * no device with that clientId has been seen in the current scan session.
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
