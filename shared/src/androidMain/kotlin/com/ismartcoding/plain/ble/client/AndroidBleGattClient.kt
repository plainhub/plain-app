package com.ismartcoding.plain.ble.client

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.os.Handler
import android.os.Looper
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.ble.BleService
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("MissingPermission")
class AndroidBleGattClient(
    val device: BluetoothDevice,
    override var rssi: Int = 0,
) : BleGattClient {

    override val id: String = device.address

    private val nameCache = mutableMapOf<String, String>()

    override val name: String?
        get() {
            val n = device.name
            if (n != null) {
                nameCache[id] = n
                return n
            }
            return nameCache[id]
        }

    var bluetoothGatt: BluetoothGatt? = null
        private set

    private val channels = mutableMapOf<ActionType, Channel<ActionResult>>()

    override fun isConnected(): Boolean = bluetoothGatt != null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            val uuid = characteristic.uuid
            when (status) {
                GATT_SUCCESS -> {
                    val strValue = try { String(value) } catch (_: Exception) { null }
                    publish(ActionType.READ, ActionResult(uuid, strValue, true))
                }
                else -> {
                    LogCat.e("Characteristic read failed for $uuid, error: $status")
                    publish(ActionType.READ, ActionResult(uuid, null, false))
                }
            }
            signalEndOfOperation()
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            val uuid = characteristic.uuid
            if (status == GATT_SUCCESS) {
                publish(ActionType.WRITE, ActionResult(uuid, null, true))
            } else {
                LogCat.e("Characteristic write failed for $uuid, error: $status")
                publish(ActionType.WRITE, ActionResult(uuid, null, false))
            }
            signalEndOfOperation()
        }

        @Suppress("DEPRECATION")
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            val uuid = descriptor.characteristic.uuid
            if (status == GATT_SUCCESS) {
                publish(ActionType.NOTIFY, ActionResult(uuid, null, true))
            } else {
                LogCat.e("Descriptor write failed for ${descriptor.characteristic.uuid}, error: $status")
                publish(ActionType.NOTIFY, ActionResult(uuid, null, false))
            }
            signalEndOfOperation()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            val uuid = characteristic.uuid
            val strValue = String(value)
            LogCat.v("[BLE] onCharacteristicChanged value $strValue for uuid $uuid")
            publish(ActionType.NOTIFY_VALUE, ActionResult(uuid, strValue, true))
        }

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int,
        ) {
            LogCat.d("[BLE] onConnectionStateChange ${gatt.device.address} status=$status newState=$newState")
            if (status == GATT_SUCCESS) {
                publish(ActionType.CONNECTION, ActionResult(null, newState.toString(), true))
                when (newState) {
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        LogCat.d("[BLE] Disconnected from ${gatt.device.address}")
                        disconnect()
                        AndroidBleScanner.teardown(this@AndroidBleGattClient)
                    }
                    BluetoothProfile.STATE_CONNECTED -> {
                        LogCat.d("[BLE] Connected to ${gatt.device.address}")
                        this@AndroidBleGattClient.bluetoothGatt = gatt
                        Handler(Looper.getMainLooper()).post {
                            gatt.discoverServices()
                        }
                    }
                }
            } else {
                LogCat.e("[BLE] ${gatt.device.address} gatt failed $status, $newState")
                publish(ActionType.CONNECTION, ActionResult(null, newState.toString(), false))
                gatt.close()
                bluetoothGatt = null
                signalEndOfOperation()
            }
        }

        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int,
        ) {
            if (status == GATT_SUCCESS) {
                val serviceCount = gatt.services?.size ?: 0
                val charUuids = gatt.services?.flatMap { s ->
                    (s.characteristics ?: emptyList()).map { it.uuid.toString().takeLast(4) }
                } ?: emptyList()
                LogCat.d("[BLE] onServicesDiscovered ${device.address}: $serviceCount services, chars=$charUuids")
                gatt.requestMtu(517)
            } else {
                LogCat.e("[BLE] onServicesDiscovered ${device.address}: FAILED status=$status")
                AndroidBleScanner.teardown(this@AndroidBleGattClient)
                signalEndOfOperation()
            }
        }

        override fun onMtuChanged(
            gatt: BluetoothGatt,
            mtu: Int,
            status: Int,
        ) {
            LogCat.d("[BLE] onMtuChanged ${gatt.device.address} mtu=$mtu status=$status")
            if (status != GATT_SUCCESS) {
                LogCat.w("[BLE] onMtuChanged ${gatt.device.address}: MTU negotiation failed status=$status, using default MTU")
            }
            publish(ActionType.MTU, ActionResult(null, null, true))
            signalEndOfOperation()
        }
    }

    override fun disconnect() {
        LogCat.d("Disconnect ${device.address} gatt=${bluetoothGatt != null}")
        bluetoothGatt?.close()
        bluetoothGatt = null
        signalEndOfOperation()
    }

    fun failOperation(type: ActionType, uuid: UUID?) {
        publish(type, ActionResult(uuid, null, false))
        signalEndOfOperation()
    }

    override suspend fun ensureConnected(retries: Int): Boolean {
        if (isConnected()) {
            LogCat.d("ensureConnected ${device.address}: already connected")
            return true
        }
        for (attempt in 0..retries) {
            LogCat.d("ensureConnected ${device.address}: attempt $attempt/$retries, gatt=${bluetoothGatt != null}")
            val operation = Operation.Connect(this)
            enqueueOperation(operation)
            val result = waitForResult(ActionType.CONNECTION, timeoutMs = 10_000L)
            LogCat.d("ensureConnected ${device.address}: attempt $attempt connection result=$result gatt=${bluetoothGatt != null}")
            if (result?.success == true && result.value == BluetoothProfile.STATE_CONNECTED.toString()) {
                val mtuResult = waitForResult(ActionType.MTU, timeoutMs = 5_000L)
                LogCat.d("ensureConnected ${device.address}: attempt $attempt mtu result=$mtuResult")
                if (mtuResult?.success == true) return true
            }
        }
        LogCat.e("ensureConnected ${device.address}: all $retries retries exhausted, gatt=${bluetoothGatt != null}")
        return false
    }

    override suspend fun writeCharacteristic(service: BleService, value: String): Boolean {
        val gatt = bluetoothGatt ?: run {
            LogCat.e("[BLE] writeCharacteristic ${service.name} ${device.address}: FAIL bluetoothGatt is null")
            return false
        }
        val charUuid = UUID.fromString(service.charUuid)
        val char = gatt.getService(UUID.fromString(service.serviceUuid))?.getCharacteristic(charUuid) ?: run {
            LogCat.e("[BLE] writeCharacteristic ${service.name} ${device.address}: FAIL characteristic not found, services=${gatt.services?.size ?: 0}")
            return false
        }
        enqueueOperation(Operation.Write(this, char, value))
        val result = waitForResult(ActionType.WRITE, charUuid, 5_000L)
        val ok = result?.success == true
        if (!ok) {
            LogCat.e("[BLE] writeCharacteristic ${service.name} ${device.address}: FAIL result=$result")
        }
        return ok
    }

    override suspend fun readCharacteristic(service: BleService): String? {
        val gatt = bluetoothGatt ?: run {
            LogCat.e("[BLE] readCharacteristic ${service.name} ${device.address}: FAIL bluetoothGatt is null")
            return null
        }
        val charUuid = UUID.fromString(service.charUuid)
        val char = gatt.getService(UUID.fromString(service.serviceUuid))?.getCharacteristic(charUuid) ?: run {
            LogCat.e("[BLE] readCharacteristic ${service.name} ${device.address}: FAIL characteristic not found, services=${gatt.services?.size ?: 0}")
            return null
        }
        enqueueOperation(Operation.Read(this, char))
        val result = waitForResult(ActionType.READ, charUuid, 10_000L)
        if (result?.success != true) {
            LogCat.e("[BLE] readCharacteristic ${service.name} ${device.address}: FAIL result=$result")
        }
        return if (result?.success == true) result.value else null
    }

    override suspend fun setNotification(service: BleService, enable: Boolean): Boolean {
        val gatt = bluetoothGatt ?: run {
            LogCat.e("[BLE] setNotification ${service.name} ${device.address}: FAIL bluetoothGatt is null")
            return false
        }
        val charUuid = UUID.fromString(service.charUuid)
        val char = gatt.getService(UUID.fromString(service.serviceUuid))?.getCharacteristic(charUuid) ?: run {
            LogCat.e("[BLE] setNotification ${service.name} ${device.address}: FAIL characteristic not found, services=${gatt.services?.size ?: 0}")
            return false
        }
        if (!gatt.setCharacteristicNotification(char, enable)) {
            LogCat.e("[BLE] setNotification ${service.name} ${device.address}: FAIL setCharacteristicNotification returned false")
            return false
        }
        val descriptor = char.descriptors.firstOrNull() ?: run {
            LogCat.e("[BLE] setNotification ${service.name} ${device.address}: FAIL no CCCD descriptor")
            return false
        }
        enqueueOperation(Operation.Notify(this, descriptor, enable))
        val result = waitForResult(ActionType.NOTIFY, charUuid, 5_000L)
        val ok = result?.success == true
        if (!ok) {
            LogCat.e("[BLE] setNotification ${service.name} ${device.address}: FAIL result=$result")
        }
        return ok
    }

    override suspend fun waitForNotification(service: BleService, timeoutMs: Long): String? {
        val charUuid = UUID.fromString(service.charUuid)
        val result = waitForResult(ActionType.NOTIFY_VALUE, charUuid, timeoutMs)
        return if (result?.success == true) result.value else null
    }

    private fun getChannel(type: ActionType): Channel<ActionResult> {
        return channels.getOrPut(type) { Channel(Channel.UNLIMITED) }
    }

    private fun publish(type: ActionType, result: ActionResult) {
        getChannel(type).trySend(result)
    }

    private suspend fun waitForResult(
        type: ActionType,
        uuid: UUID? = null,
        timeoutMs: Long = 5_000L,
    ): ActionResult? {
        val tag = "[BLE] waitForResult ${type} ${device.address}"
        val result = withTimeoutOrNull(timeoutMs.milliseconds) {
            val channel = getChannel(type)
            var result = channel.receive()
            while (uuid != null && result.uuid != uuid) {
                LogCat.e("$tag STALE: got uuid=${result.uuid} success=${result.success}, expecting $uuid (draining)")
                result = channel.receive()
            }
            result
        }
        if (result == null) {
            LogCat.e("$tag TIMEOUT after ${timeoutMs}ms, expecting uuid=$uuid")
        }
        return result
    }

    enum class ActionType { READ, WRITE, NOTIFY, NOTIFY_VALUE, CONNECTION, MTU }

    data class ActionResult(
        val uuid: UUID?,
        val value: String?,
        val success: Boolean,
    )

    sealed class Operation {
        abstract val client: AndroidBleGattClient

        class Connect(override val client: AndroidBleGattClient) : Operation() {
            override fun run() {
                client.device.connectGatt(appContext, false, client.gattCallback, BluetoothDevice.TRANSPORT_LE)
            }
        }

        class Write(
            override val client: AndroidBleGattClient,
            val char: BluetoothGattCharacteristic,
            val value: String,
        ) : Operation() {
            @Suppress("DEPRECATION")
            override fun run() {
                char.setValue(value)
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                val ok = client.bluetoothGatt?.writeCharacteristic(char) ?: false
                LogCat.d("[BLE] Write op ${client.device.address} charUuid=${char.uuid} valueLen=${value.length} writeCharacteristic=$ok")
                if (!ok) {
                    LogCat.e("[BLE] Write op ${client.device.address}: writeCharacteristic returned false, failing operation")
                    client.failOperation(ActionType.WRITE, char.uuid)
                }
            }
        }

        class Read(
            override val client: AndroidBleGattClient,
            val char: BluetoothGattCharacteristic,
        ) : Operation() {
            override fun run() {
                val ok = client.bluetoothGatt?.readCharacteristic(char) ?: false
                LogCat.d("[BLE] Read op ${client.device.address} charUuid=${char.uuid} readCharacteristic=$ok")
                if (!ok) {
                    LogCat.e("[BLE] Read op ${client.device.address}: readCharacteristic returned false, failing operation")
                    client.failOperation(ActionType.READ, char.uuid)
                }
            }
        }

        class Notify(
            override val client: AndroidBleGattClient,
            val descriptor: BluetoothGattDescriptor,
            val enable: Boolean,
        ) : Operation() {
            @Suppress("DEPRECATION")
            override fun run() {
                descriptor.value = if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                val ok = client.bluetoothGatt?.writeDescriptor(descriptor) ?: false
                LogCat.d("[BLE] Notify op ${client.device.address} charUuid=${descriptor.characteristic.uuid} enable=$enable writeDescriptor=$ok")
                if (!ok) {
                    LogCat.e("[BLE] Notify op ${client.device.address}: writeDescriptor returned false, failing operation")
                    client.failOperation(ActionType.NOTIFY, descriptor.characteristic.uuid)
                }
            }
        }

        abstract fun run()
    }

    companion object {
        private val operationQueue = ConcurrentLinkedQueue<Operation>()
        @Volatile
        private var pendingOperation: Operation? = null

        @Synchronized
        private fun enqueueOperation(operation: Operation) {
            operationQueue.add(operation)
            if (pendingOperation == null) {
                doNextOperation()
            }
        }

        @Synchronized
        private fun signalEndOfOperation() {
            pendingOperation = null
            if (operationQueue.isNotEmpty()) {
                doNextOperation()
            }
        }

        @Synchronized
        private fun doNextOperation() {
            if (pendingOperation != null) return
            val operation = operationQueue.poll() ?: return
            pendingOperation = operation
            if (operation is Operation.Connect) {
                operation.run()
                return
            }
            if (!operation.client.isConnected()) {
                signalEndOfOperation()
                return
            }
            operation.run()
        }
    }
}
