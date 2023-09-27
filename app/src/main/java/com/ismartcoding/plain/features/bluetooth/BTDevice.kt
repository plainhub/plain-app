package com.ismartcoding.plain.features.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.os.Handler
import android.os.Looper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import kotlinx.coroutines.channels.Channel
import org.json.JSONObject
import java.util.*

@SuppressLint("MissingPermission")
open class BTDevice(val device: BluetoothDevice, var rssi: Int = 0) {
    val mac: String = device.address
    private val channels = mutableMapOf<BluetoothActionType, Channel<BluetoothResult>>()
    val notificationCache =
        mapOf<UUID, MutableList<String>>(
            AUTH_CHAR_UUID to mutableListOf(),
            PRE_AUTH_CHAR_UUID to mutableListOf(),
            GRAPHQL_CHAR_UUID to mutableListOf(),
        )
    var bluetoothGatt: BluetoothGatt? = null
        private set

    fun isConnected(): Boolean {
        return bluetoothGatt != null
    }

    private val gattCallback =
        object : BluetoothGattCallback() {
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                with(characteristic) {
                    when (status) {
                        GATT_SUCCESS -> {
                            try {
                                val jsonData = characteristic.getStringValue(0)
                                val json = JSONObject(jsonData)
                                publish(BluetoothActionType.CHARACTERISTIC_READ, BluetoothResult(uuid, json, BluetoothActionResult.SUCCESS))
                            } catch (ex: Exception) {
                                LogCat.e("Failed to parse json data: $value, error: $ex")
                                publish(BluetoothActionType.CHARACTERISTIC_READ, BluetoothResult(uuid, null, BluetoothActionResult.FAIL))
                            }
                        }
                        else -> {
                            LogCat.e("Characteristic read failed for $uuid, error: $status")
                            publish(BluetoothActionType.CHARACTERISTIC_READ, BluetoothResult(uuid, null, BluetoothActionResult.FAIL))
                        }
                    }
                }

                if (BluetoothUtil.pendingOperation is BTOperationCharacteristicRead) {
                    BluetoothUtil.signalEndOfOperation()
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                with(characteristic) {
                    when (status) {
                        GATT_SUCCESS -> {
                            publish(BluetoothActionType.CHARACTERISTIC_WRITE, BluetoothResult(uuid, null, BluetoothActionResult.SUCCESS))
                        }
                        else -> {
                            LogCat.e("Characteristic write failed for $uuid, error: $status")
                            publish(BluetoothActionType.CHARACTERISTIC_WRITE, BluetoothResult(uuid, null, BluetoothActionResult.FAIL))
                        }
                    }
                }

                if (BluetoothUtil.pendingOperation is BTOperationCharacteristicWrite) {
                    BluetoothUtil.signalEndOfOperation()
                }
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int,
            ) {
                with(descriptor) {
                    when (status) {
                        GATT_SUCCESS -> {
                            publish(
                                BluetoothActionType.DESCRIPTOR_WRITE,
                                BluetoothResult(characteristic.uuid, status, BluetoothActionResult.SUCCESS),
                            )
                        }
                        else -> {
                            LogCat.e("Descriptor write failed for ${characteristic.uuid}, error: $status")
                            publish(
                                BluetoothActionType.DESCRIPTOR_WRITE,
                                BluetoothResult(characteristic.uuid, status, BluetoothActionResult.FAIL),
                            )
                        }
                    }
                }

                if (BluetoothUtil.pendingOperation is BTOperationEnableNotifications) {
                    BluetoothUtil.signalEndOfOperation()
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
            ) {
                with(characteristic) {
                    val strValue = String(value)
                    LogCat.v("Got notification value $strValue for uuid $uuid")
                    notificationCache[uuid]?.add(strValue)
                }
            }

            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int,
            ) {
                if (status == GATT_SUCCESS) {
                    publish(BluetoothActionType.CONNECTION_STATE, BluetoothResult(null, newState, BluetoothActionResult.SUCCESS))
                    when (newState) {
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            LogCat.d("Disconnected from ${gatt.device.address}")
                            disconnect() // make sure gatt is disposed when the connection is terminated accidentally.
                            BluetoothUtil.teardownConnection(this@BTDevice)
                        }
                        BluetoothProfile.STATE_CONNECTED -> {
                            LogCat.d("Connected to ${gatt.device.address}")
                            this@BTDevice.bluetoothGatt = gatt
                            Handler(Looper.getMainLooper()).post {
                                gatt.discoverServices()
                            }
                        }
                        else -> {
                            LogCat.e("Other state $newState")
                        }
                    }
                } else {
                    LogCat.e("${gatt.device.address} gatt failed $status, $newState")
                    publish(BluetoothActionType.CONNECTION_STATE, BluetoothResult(null, newState, BluetoothActionResult.FAIL))
                    gatt.close()
                    this@BTDevice.bluetoothGatt = null
                    if (BluetoothUtil.pendingOperation is BTOperationConnect) {
                        BluetoothUtil.signalEndOfOperation()
                    }
                }
            }

            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int,
            ) {
                with(gatt) {
                    if (status == GATT_SUCCESS) {
                        LogCat.d("Discovered ${services.size} services for ${device.address}.")
                        BluetoothUtil.requestMtu(this@BTDevice, 517)
                        val str = mutableListOf<String>()
                        gatt.services?.forEach { s ->
                            val serviceString = s.uuid.toString()
                            s.characteristics.forEach { c ->
                                val charString = c.uuid.toString()
                                str.add("[$serviceString + $charString]")
                            }
                        }
                        LogCat.d("Discover services: " + str.joinToString(", "))
                    } else {
                        LogCat.e("Service discovery failed, status $status")
                        BluetoothUtil.teardownConnection(this@BTDevice)
                    }
                }

                if (BluetoothUtil.pendingOperation is BTOperationConnect) {
                    BluetoothUtil.signalEndOfOperation()
                }
            }

            override fun onMtuChanged(
                gatt: BluetoothGatt,
                mtu: Int,
                status: Int,
            ) {
                LogCat.d("${gatt.device.address} MTU is changed to $mtu, status: $status")
                publish(BluetoothActionType.MTU, BluetoothResult(null, null, BluetoothActionResult.SUCCESS))
                if (BluetoothUtil.pendingOperation is BTOperationMtuRequest) {
                    BluetoothUtil.signalEndOfOperation()
                }
            }
        }

    fun disconnect() {
        LogCat.d("Disconnect ${device.address}")
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    fun connect() {
        LogCat.d("connecting ${device.address}")
        device.connectGatt(MainApp.instance, false, this.gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun readCharacteristic(api: BluetoothApi): Boolean {
        val char = getChar(api) ?: return false
        bluetoothGatt?.readCharacteristic(char)
        return true
    }

    fun writeCharacteristic(
        api: BluetoothApi,
        value: String,
    ): Boolean {
        val char = getChar(api) ?: return false
        char.setValue(value)
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        bluetoothGatt?.writeCharacteristic(char)
        return true
    }

    fun enableNotification(
        api: BluetoothApi,
        enable: Boolean,
    ): Boolean {
        val char = getChar(api) ?: return false
        if (bluetoothGatt?.setCharacteristicNotification(char, enable) == false) {
            return false
        }

        val descriptor = char.descriptors[0]
        descriptor.value = if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        bluetoothGatt?.writeDescriptor(descriptor)

        return true
    }

    fun getChannel(type: BluetoothActionType): Channel<BluetoothResult> {
        return channels.getOrPut(type, { Channel() })
    }

    private fun getChar(api: BluetoothApi): BluetoothGattCharacteristic? {
        return bluetoothGatt?.getService(api.serviceUUID)?.getCharacteristic(api.charUUID)
    }

    private fun publish(
        type: BluetoothActionType,
        result: BluetoothResult,
    ) {
        getChannel(type).trySend(result)
        LogCat.d("Adding result to channel $type $result")
    }

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("47fb7d7c-24fb-4660-8293-6cab94ba0cfe")
        private val GRAPHQL_CHAR_UUID = UUID.fromString("93169e97-e00d-470c-9413-013077fcdb98")
        private val AUTH_CHAR_UUID = UUID.fromString("2ee565e3-ac94-476b-8503-c1f2606f9310")
        private val PRE_AUTH_CHAR_UUID = UUID.fromString("b0f304dd-f102-445d-bca1-7b48a4aa4e42")

        val graphqlService = BluetoothApi("graphqlService", SERVICE_UUID, GRAPHQL_CHAR_UUID)
        val preAuthService = BluetoothApi("preAuthService", SERVICE_UUID, PRE_AUTH_CHAR_UUID)
        val authService = BluetoothApi("authService", SERVICE_UUID, AUTH_CHAR_UUID)
    }
}

data class BluetoothApi(val name: String, val serviceUUID: UUID, val charUUID: UUID)

data class BluetoothResult(val uuid: UUID?, val value: Any?, val status: BluetoothActionResult) {
    fun isSuccess(): Boolean {
        return status == BluetoothActionResult.SUCCESS
    }
}
