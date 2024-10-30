package com.ismartcoding.plain.features.bluetooth

import android.bluetooth.BluetoothProfile
import com.ismartcoding.lib.logcat.LogCat
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.TimeUnit

class SmartBTDevice(val device: BTDevice) {
    val name: String
        get() {
            return device.device.name
        }

    fun isConnected(): Boolean {
        return device.isConnected()
    }

    fun disconnect() {
        BluetoothUtil.teardownConnection(device)
    }

    suspend fun ensureConnectedAsync(retry: Int = 3) {
        if (!device.isConnected()) {
            LogCat.d("ensureConnectedAsync: $retry")
            BluetoothUtil.connect(device)
            if (!isConnectedAsync() && retry > 0) {
                ensureConnectedAsync(retry - 1)
            }
        }
    }

    suspend fun requestAsync(
        api: BluetoothApi,
        requestData: BleRequestData,
    ): BluetoothResult {
        val chunked = requestData.toJSON().toString().chunked(460)
        chunked.forEachIndexed { index, value ->
            val r =
                writeCharacteristicAsync(
                    device,
                    api,
                    BleSegmentData.build(value, start = index == 0, end = index == chunked.size - 1).toJSON().toString(),
                )
            if (!r.isSuccess()) {
                return r
            }
        }

        var r = enableNotificationsAsync(device, api, true)
        if (!r.isSuccess()) {
            return r
        }

        var responseStr = ""
        var i = 0
        while (i < 50) {
            val valueList = device.notificationCache[api.charUUID]!!
            if (valueList.isNotEmpty()) {
                try {
                    val lastData = BleSegmentData.fromJSON(valueList.last())
                    if (lastData.isEnd()) {
                        valueList.forEach {
                            val data = BleSegmentData.fromJSON(it)
                            responseStr += data.data
                        }
                        valueList.clear()
                        break
                    } else {
                        delay(500)
                    }
                } catch (ex: Exception) {
                    valueList.clear()
                    LogCat.e(ex.toString())
                    break
                }
            } else {
                delay(500)
            }
            i++
        }

        r = enableNotificationsAsync(device, api, false)
        if (!r.isSuccess()) {
            return r
        }

        return BluetoothResult(api.charUUID, responseStr, BluetoothActionResult.SUCCESS)
    }

    private suspend fun readCharacteristicAsync(
        device: BTDevice,
        api: BluetoothApi,
    ): BluetoothResult {
        if (device.isConnected()) {
            BluetoothUtil.enqueueOperation(BTOperationCharacteristicRead(device, api))
            return waitForResultAsync(BluetoothActionType.CHARACTERISTIC_READ, api.charUUID)
        } else {
            LogCat.e("Not connected to ${device.mac}, cannot read characteristic!")
        }

        return BluetoothResult(api.charUUID, null, BluetoothActionResult.FAIL)
    }

    private suspend fun writeCharacteristicAsync(
        device: BTDevice,
        api: BluetoothApi,
        value: String,
    ): BluetoothResult {
        if (device.isConnected()) {
            BluetoothUtil.enqueueOperation(BTOperationCharacteristicWrite(device, api, value))
            return waitForResultAsync(BluetoothActionType.CHARACTERISTIC_WRITE, api.charUUID)
        } else {
            LogCat.e("Not connected to ${device.mac}, cannot write characteristic!")
        }

        return BluetoothResult(api.charUUID, null, BluetoothActionResult.FAIL)
    }

    private suspend fun enableNotificationsAsync(
        device: BTDevice,
        api: BluetoothApi,
        enable: Boolean,
    ): BluetoothResult {
        if (device.isConnected()) {
            BluetoothUtil.enqueueOperation(BTOperationEnableNotifications(device, api, enable))
            return waitForResultAsync(BluetoothActionType.DESCRIPTOR_WRITE, api.charUUID)
        } else {
            LogCat.e("Not connected to ${device.mac}, cannot ${if (enable) "enable" else "disable"} notifications!")
        }

        return BluetoothResult(api.charUUID, null, BluetoothActionResult.FAIL)
    }

    private suspend fun waitForResultAsync(
        type: BluetoothActionType,
        uuid: UUID?,
        timeout: Long = 2,
    ): BluetoothResult {
        return withTimeoutOrNull(TimeUnit.SECONDS.toMillis(timeout)) {
            val channel = device.getChannel(type)
            var bluetoothResult = channel.receive()
            while (bluetoothResult.uuid != uuid) {
                LogCat.e("Got a $type reply for uuid ${bluetoothResult.uuid}, expecting uuid $uuid")
                bluetoothResult = channel.receive()
            }
            bluetoothResult
        } ?: run {
            return BluetoothResult(uuid, null, BluetoothActionResult.TIMEOUT)
        }
    }

    private suspend fun isConnectedAsync(): Boolean {
        val result = waitForResultAsync(BluetoothActionType.CONNECTION_STATE, null)
        if (result.value == BluetoothProfile.STATE_CONNECTED) {
            waitForResultAsync(BluetoothActionType.MTU, null, 5)
            return true
        }
        return false
    }
}
