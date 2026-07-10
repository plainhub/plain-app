package com.ismartcoding.plain.ble.client

import com.ismartcoding.plain.ble.BleService
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegate
import platform.CoreBluetooth.CBPeripheralStateConnected
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.CoreBluetooth.CBCharacteristicWriteTypeWithResponse
import platform.Foundation.NSData
import platform.Foundation.NSError
import kotlin.time.Duration.Companion.milliseconds

class IosBleGattClient(
    val peripheral: CBPeripheral,
    override var rssi: Int = 0,
) : BleGattClient {

    override val id: String = peripheral.identifier.UUIDString
    override val name: String? get() = peripheral.name

    private val delegate = PeripheralDelegate(this)

    private var servicesDiscovered = false
    private var serviceDiscoveryDeferred: CompletableDeferred<Boolean>? = null
    private var charDiscoveryDeferred: CompletableDeferred<Boolean>? = null

    private var writeDeferred: CompletableDeferred<Boolean>? = null
    private var readDeferred: CompletableDeferred<String?>? = null
    private var notifyStateDeferred: CompletableDeferred<Boolean>? = null
    private var notifyValueDeferred: CompletableDeferred<String?>? = null

    private val characteristics = mutableMapOf<String, CBCharacteristic>()

    init {
        peripheral.delegate = delegate
    }

    override fun isConnected(): Boolean = peripheral.state == CBPeripheralStateConnected

    override suspend fun ensureConnected(retries: Int): Boolean {
        if (isConnected() && servicesDiscovered) return true
        for (attempt in 0..retries) {
            LogCat.d("ensureConnected: attempt $attempt for $id")
            val connected = IosBleScanner.connectPeripheral(this)
            if (connected && discoverServicesAndCharacteristics()) {
                servicesDiscovered = true
                return true
            }
        }
        return false
    }

    private suspend fun discoverServicesAndCharacteristics(): Boolean {
        val serviceDeferred = CompletableDeferred<Boolean>()
        serviceDiscoveryDeferred = serviceDeferred
        peripheral.discoverServices(listOf(CBUUID.UUIDWithString(BleUuids.SERVICE_UUID)))
        val serviceOk = withTimeoutOrNull(10_000L.milliseconds) { serviceDeferred.await() } == true
        serviceDiscoveryDeferred = null
        if (!serviceOk) return false

        val service = peripheral.services?.firstOrNull {
            it.UUID.UUIDString.equals(BleUuids.SERVICE_UUID, ignoreCase = true)
        } ?: return false

        val charDeferred = CompletableDeferred<Boolean>()
        charDiscoveryDeferred = charDeferred
        val allCharUuids = listOf(
            BleUuids.NEARBY_CHAR_UUID,
            BleUuids.RPC_CHAR_UUID,
        ).map { CBUUID.UUIDWithString(it) }
        peripheral.discoverCharacteristicsForService(allCharUuids, service)
        val charOk = withTimeoutOrNull(10_000L.milliseconds) { charDeferred.await() } == true
        charDiscoveryDeferred = null
        return charOk
    }

    private fun getCharacteristic(service: BleService): CBCharacteristic? {
        characteristics[service.charUuid]?.let { return it }
        val cbService = peripheral.services?.firstOrNull {
            it.UUID.UUIDString.equals(service.serviceUuid, ignoreCase = true)
        } ?: return null
        val char = cbService.characteristics?.firstOrNull {
            it.UUID.UUIDString.equals(service.charUuid, ignoreCase = true)
        }
        if (char != null) {
            characteristics[service.charUuid] = char
        }
        return char
    }

    override suspend fun writeCharacteristic(service: BleService, value: String): Boolean {
        val char = getCharacteristic(service) ?: return false
        val data = value.encodeToByteArray().toNSData()
        val deferred = CompletableDeferred<Boolean>()
        writeDeferred = deferred
        peripheral.writeValue(data, forCharacteristic = char, type = CBCharacteristicWriteTypeWithResponse)
        val result = withTimeoutOrNull(5_000L.milliseconds) { deferred.await() }
        writeDeferred = null
        return result == true
    }

    override suspend fun readCharacteristic(service: BleService): String? {
        val char = getCharacteristic(service) ?: return null
        val deferred = CompletableDeferred<String?>()
        readDeferred = deferred
        peripheral.readValueForCharacteristic(char)
        val result = withTimeoutOrNull(10_000L.milliseconds) { deferred.await() }
        readDeferred = null
        return result
    }

    override suspend fun setNotification(service: BleService, enable: Boolean): Boolean {
        val char = getCharacteristic(service) ?: return false
        val deferred = CompletableDeferred<Boolean>()
        notifyStateDeferred = deferred
        peripheral.setNotifyValue(enable, char)
        val result = withTimeoutOrNull(5_000L.milliseconds) { deferred.await() }
        notifyStateDeferred = null
        return result == true
    }

    override suspend fun waitForNotification(service: BleService, timeoutMs: Long): String? {
        val deferred = CompletableDeferred<String?>()
        notifyValueDeferred = deferred
        val result = withTimeoutOrNull(timeoutMs.milliseconds) { deferred.await() }
        notifyValueDeferred = null
        return result
    }

    override fun disconnect() {
        IosBleScanner.teardownConnection(this)
    }

    internal fun onServicesDiscovered(error: NSError?) {
        serviceDiscoveryDeferred?.complete(error == null)
    }

    internal fun onCharacteristicsDiscovered(error: NSError?) {
        peripheral.services?.forEach { service ->
            service.characteristics?.forEach { char ->
                characteristics[char.UUID.UUIDString] = char
            }
        }
        charDiscoveryDeferred?.complete(error == null)
    }

    internal fun onCharacteristicWrite(error: NSError?) {
        writeDeferred?.complete(error == null)
    }

    internal fun onCharacteristicUpdated(char: CBCharacteristic, error: NSError?) {
        val value = if (error == null) {
            char.value?.toByteArray()?.decodeToString()
        } else null

        val notifyDeferred = notifyValueDeferred
        if (notifyDeferred != null && notifyDeferred.isActive) {
            notifyDeferred.complete(value)
            return
        }

        val readDeferred = readDeferred
        if (readDeferred != null && readDeferred.isActive) {
            readDeferred.complete(value)
        }
    }

    internal fun onNotificationStateChanged(error: NSError?) {
        notifyStateDeferred?.complete(error == null)
    }
}

private class PeripheralDelegate(private val client: IosBleGattClient) : CBPeripheralDelegate() {
    override fun peripheralDidDiscoverServices(peripheral: CBPeripheral, error: NSError?) {
        client.onServicesDiscovered(error)
    }

    override fun peripheralDidDiscoverCharacteristicsForService(
        peripheral: CBPeripheral,
        service: CBService,
        error: NSError?,
    ) {
        client.onCharacteristicsDiscovered(error)
    }

    override fun peripheralDidUpdateValueForCharacteristic(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: NSError?,
    ) {
        client.onCharacteristicUpdated(characteristic, error)
    }

    override fun peripheralDidWriteValueForCharacteristic(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: NSError?,
    ) {
        client.onCharacteristicWrite(error)
    }

    override fun peripheralDidUpdateNotificationStateForCharacteristic(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: NSError?,
    ) {
        client.onNotificationStateChanged(error)
    }
}

private fun NSData.toByteArray(): ByteArray = ByteArray(this.length.toInt()).also { buf ->
    this.getBytes(buf)
}
