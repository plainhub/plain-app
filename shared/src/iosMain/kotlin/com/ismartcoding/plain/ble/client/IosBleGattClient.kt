@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.ble.client

import com.ismartcoding.plain.ble.BleService
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.lib.toNSData
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBPeripheralStateConnected
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds

class IosBleGattClient(
    val peripheral: CBPeripheral,
    override var rssi: Int = 0,
    override var awareFlags: Int = 0,
    clientId: String = "",
) : BleGattClient {

    /**
     * Stable peer identifier — the peer's clientId (TempData.clientId, a 13-char
     * short UUID) parsed from the BLE advertisement serviceData. Falls back to
     * [peripheral.identifier] when no clientId was advertised (e.g. peer is
     * running an older app version that doesn't broadcast clientId).
     */
    override val id: String = clientId.ifEmpty { peripheral.identifier.UUIDString }
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
            (it as CBService).UUID.UUIDString.equals(BleUuids.SERVICE_UUID, ignoreCase = true)
        } as? CBService ?: return false

        val charDeferred = CompletableDeferred<Boolean>()
        charDiscoveryDeferred = charDeferred
        val allCharUuids = listOf(
            BleUuids.NEARBY_CHAR_UUID,
            BleUuids.RPC_CHAR_UUID,
        ).map { CBUUID.UUIDWithString(it) }
        peripheral.discoverCharacteristics(allCharUuids, forService = service)
        val charOk = withTimeoutOrNull(10_000L.milliseconds) { charDeferred.await() } == true
        charDiscoveryDeferred = null
        return charOk
    }

    private fun getCharacteristic(service: BleService): CBCharacteristic? {
        characteristics[service.charUuid]?.let { return it }
        val cbService = peripheral.services?.firstOrNull {
            (it as CBService).UUID.UUIDString.equals(service.serviceUuid, ignoreCase = true)
        } as? CBService ?: return null
        val char = cbService.characteristics?.firstOrNull {
            (it as CBCharacteristic).UUID.UUIDString.equals(service.charUuid, ignoreCase = true)
        } as? CBCharacteristic
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
        peripheral.writeValue(data, forCharacteristic = char, type = 0L)
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
        peripheral.services?.forEach { svc ->
            val service = svc as CBService
            service.characteristics?.forEach { ch ->
                val char = ch as CBCharacteristic
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

private class PeripheralDelegate(private val client: IosBleGattClient) : NSObject(),
    CBPeripheralDelegateProtocol {
    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        client.onServicesDiscovered(didDiscoverServices)
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?,
    ) {
        client.onCharacteristicsDiscovered(error)
    }

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateValueForCharacteristic: CBCharacteristic,
        error: NSError?,
    ) {
        client.onCharacteristicUpdated(didUpdateValueForCharacteristic, error)
    }

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?,
    ) {
        client.onCharacteristicWrite(error)
    }

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateNotificationStateForCharacteristic: CBCharacteristic,
        error: NSError?,
    ) {
        client.onNotificationStateChanged(error)
    }
}
