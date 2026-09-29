package com.ismartcoding.plain.ble.client

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.os.Handler
import android.os.Looper
import com.ismartcoding.plain.ble.BleService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("MissingPermission")
class AndroidBleGattClient(
    val device: BluetoothDevice,
    private val context: Context,
    private val logger: (String, String) -> Unit = { _, _ -> },
    private val onDisconnected: (AndroidBleGattClient) -> Unit = {},
    override var rssi: Int = 0,
    shortId: String = "",
    override val awareSupported: Boolean = false,
    override val awareRunning: Boolean = false,
) : BleGattClient {

    /**
     * Stable peer match key — the 8-byte truncated SHA256 of the peer's full
     * clientId, rendered as a 16-char hex string. Parsed from the BLE scan
     * response serviceData via [com.ismartcoding.plain.ble.BleServiceData].
     * NOT the BLE MAC, because Android randomizes BLE MACs every ~15 minutes.
     * Falls back to the MAC when no serviceData was advertised.
     */
    override val id: String = shortId.ifEmpty { device.address }

    /**
     * The current BLE MAC of this device, used internally for Android GATT
     * connections (logging, debugging). Randomized by Android every ~15 min,
     * so never persist this or use it as a peer id.
     */
    val mac: String = device.address

    private fun debug(message: String) = logger("D", message)
    private fun warning(message: String) = logger("W", message)
    private fun error(message: String) = logger("E", message)

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

    // Per-connection operation queue. This MUST be an instance member (not
    // static): Android allows concurrent operations across independent GATT
    // connections, so a slow connect on one device must never block writes on
    // another (previously a global queue let a stuck Connect starve pairing
    // descriptor writes into a timeout). All access is serialized by the
    // @Synchronized queue methods below.
    private val operationQueue = ArrayDeque<Operation>()
    private var pendingOperation: Operation? = null

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
                    error("Characteristic read failed for $uuid, error: $status")
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
                error("Characteristic write failed for $uuid, error: $status")
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
                error("Descriptor write failed for ${descriptor.characteristic.uuid}, error: $status")
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
//            LogCat.v("[BLE] onCharacteristicChanged value $strValue for uuid $uuid")
            publish(ActionType.NOTIFY_VALUE, ActionResult(uuid, strValue, true))
        }

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int,
        ) {
            debug("[BLE] onConnectionStateChange ${mac} status=$status newState=$newState")
            if (status == GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        debug("[BLE] Disconnected from ${mac}")
                        disconnect()
                        onDisconnected(this@AndroidBleGattClient)
                    }
                    BluetoothProfile.STATE_CONNECTED -> {
                        debug("[BLE] Connected to ${mac}")
                        // Bind bluetoothGatt BEFORE publishing the CONNECTION
                        // result so ensureConnected() only observes success once
                        // isConnected() is true (avoids a race where the channel
                        // handoff resumes ahead of the field assignment).
                        this@AndroidBleGattClient.bluetoothGatt = gatt
                        Handler(Looper.getMainLooper()).post {
                            gatt.discoverServices()
                        }
                    }
                }
                publish(ActionType.CONNECTION, ActionResult(null, newState.toString(), true))
            } else {
                error("[BLE] ${mac} gatt failed $status, $newState")
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
                debug("[BLE] onServicesDiscovered ${mac}: $serviceCount services, chars=$charUuids")
                gatt.requestMtu(517)
            } else {
                error("[BLE] onServicesDiscovered ${mac}: FAILED status=$status")
                onDisconnected(this@AndroidBleGattClient)
                signalEndOfOperation()
            }
        }

        override fun onMtuChanged(
            gatt: BluetoothGatt,
            mtu: Int,
            status: Int,
        ) {
            debug("[BLE] onMtuChanged ${mac} mtu=$mtu status=$status")
            if (status != GATT_SUCCESS) {
                warning("[BLE] onMtuChanged ${mac}: MTU negotiation failed status=$status, using default MTU")
            }
            publish(ActionType.MTU, ActionResult(null, null, true))
            signalEndOfOperation()
        }
    }

    override fun disconnect() {
        debug("Disconnect ${mac} gatt=${bluetoothGatt != null}")
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
            debug("ensureConnected ${mac}: already connected")
            return true
        }
        for (attempt in 0..retries) {
            debug("ensureConnected ${mac}: attempt $attempt/$retries, gatt=${bluetoothGatt != null}")
            val operation = Operation.Connect(this)
            enqueueOperation(operation)
            val result = waitForResult(ActionType.CONNECTION, timeoutMs = 10_000L)
            debug("ensureConnected ${mac}: attempt $attempt connection result=$result gatt=${bluetoothGatt != null}")
            if (result?.success == true && result.value == BluetoothProfile.STATE_CONNECTED.toString()) {
                val mtuResult = waitForResult(ActionType.MTU, timeoutMs = 5_000L)
                debug("ensureConnected ${mac}: attempt $attempt mtu result=$mtuResult")
                // Only report connected once the GATT is actually bound.
                // A stale MTU result left in the channel from a previous
                // connection must not make us skip the real connection.
                if (mtuResult?.success == true && bluetoothGatt != null) return true
            }
        }
        error("ensureConnected ${mac}: all $retries retries exhausted, gatt=${bluetoothGatt != null}")
        return false
    }

    override suspend fun writeCharacteristic(service: BleService, value: String): Boolean {
        val gatt = bluetoothGatt ?: run {
            error("[BLE] writeCharacteristic ${service.name} ${mac}: FAIL bluetoothGatt is null")
            return false
        }
        val charUuid = UUID.fromString(service.charUuid)
        val char = gatt.getService(UUID.fromString(service.serviceUuid))?.getCharacteristic(charUuid) ?: run {
            error("[BLE] writeCharacteristic ${service.name} ${mac}: FAIL characteristic not found, services=${gatt.services?.size ?: 0}")
            return false
        }
        enqueueOperation(Operation.Write(this, char, value))
        val result = waitForResult(ActionType.WRITE, charUuid, 5_000L)
        val ok = result?.success == true
        if (!ok) {
            error("[BLE] writeCharacteristic ${service.name} ${mac}: FAIL result=$result")
        }
        return ok
    }

    override suspend fun readCharacteristic(service: BleService): String? {
        val gatt = bluetoothGatt ?: run {
            error("[BLE] readCharacteristic ${service.name} ${mac}: FAIL bluetoothGatt is null")
            return null
        }
        val charUuid = UUID.fromString(service.charUuid)
        val char = gatt.getService(UUID.fromString(service.serviceUuid))?.getCharacteristic(charUuid) ?: run {
            error("[BLE] readCharacteristic ${service.name} ${mac}: FAIL characteristic not found, services=${gatt.services?.size ?: 0}")
            return null
        }
        enqueueOperation(Operation.Read(this, char))
        val result = waitForResult(ActionType.READ, charUuid, 10_000L)
        if (result?.success != true) {
            error("[BLE] readCharacteristic ${service.name} ${mac}: FAIL result=$result")
        }
        return if (result?.success == true) result.value else null
    }

    override suspend fun setNotification(service: BleService, enable: Boolean): Boolean {
        val gatt = bluetoothGatt ?: run {
            error("[BLE] setNotification ${service.name} ${mac}: FAIL bluetoothGatt is null")
            return false
        }
        val charUuid = UUID.fromString(service.charUuid)
        val char = gatt.getService(UUID.fromString(service.serviceUuid))?.getCharacteristic(charUuid) ?: run {
            error("[BLE] setNotification ${service.name} ${mac}: FAIL characteristic not found, services=${gatt.services?.size ?: 0}")
            return false
        }
        if (!gatt.setCharacteristicNotification(char, enable)) {
            error("[BLE] setNotification ${service.name} ${mac}: FAIL setCharacteristicNotification returned false")
            return false
        }
        val descriptor = char.descriptors.firstOrNull() ?: run {
            error("[BLE] setNotification ${service.name} ${mac}: FAIL no CCCD descriptor")
            return false
        }
        enqueueOperation(Operation.Notify(this, descriptor, enable))
        val result = waitForResult(ActionType.NOTIFY, charUuid, 5_000L)
        val ok = result?.success == true
        if (!ok) {
            error("[BLE] setNotification ${service.name} ${mac}: FAIL result=$result")
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
        val tag = "[BLE] waitForResult ${type} ${mac}"
        val result = withTimeoutOrNull(timeoutMs.milliseconds) {
            val channel = getChannel(type)
            var result = channel.receive()
            while (uuid != null && result.uuid != uuid) {
                error("$tag STALE: got uuid=${result.uuid} success=${result.success}, expecting $uuid (draining)")
                result = channel.receive()
            }
            result
        }
        if (result == null) {
            error("$tag TIMEOUT after ${timeoutMs}ms, expecting uuid=$uuid")
        }
        return result
    }

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
        val operation = operationQueue.removeFirstOrNull() ?: return
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
                client.device.connectGatt(client.context, false, client.gattCallback, BluetoothDevice.TRANSPORT_LE)
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
                client.debug("[BLE] Write op ${client.mac} charUuid=${char.uuid} valueLen=${value.length} writeCharacteristic=$ok")
                if (!ok) {
                    client.error("[BLE] Write op ${client.mac}: writeCharacteristic returned false, failing operation")
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
                client.debug("[BLE] Read op ${client.mac} charUuid=${char.uuid} readCharacteristic=$ok")
                if (!ok) {
                    client.error("[BLE] Read op ${client.mac}: readCharacteristic returned false, failing operation")
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
                client.debug("[BLE] Notify op ${client.mac} charUuid=${descriptor.characteristic.uuid} enable=$enable writeDescriptor=$ok")
                if (!ok) {
                    client.error("[BLE] Notify op ${client.mac}: writeDescriptor returned false, failing operation")
                    client.failOperation(ActionType.NOTIFY, descriptor.characteristic.uuid)
                }
            }
        }

        abstract fun run()
    }
}
