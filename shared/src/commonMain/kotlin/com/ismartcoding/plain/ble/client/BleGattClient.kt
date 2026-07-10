package com.ismartcoding.plain.ble.client

import com.ismartcoding.plain.ble.BleService
import kotlinx.coroutines.flow.Flow

interface BleGattClient {
    val id: String
    val name: String?
    var rssi: Int

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

    fun createClient(mac: String): BleGattClient?

    fun isReadyToUse(): Boolean

    fun isAdvertiseReady(): Boolean

    suspend fun ensurePermission(): Boolean

    fun teardownConnection(device: BleGattClient)
}
