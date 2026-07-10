package com.ismartcoding.plain.ble

enum class BleActionResult { SUCCESS, FAIL, TIMEOUT }

data class BleResult(
    val charUuid: String?,
    val value: Any?,
    val status: BleActionResult,
) {
    fun isSuccess(): Boolean = status == BleActionResult.SUCCESS
}

data class BleService(
    val name: String,
    val serviceUuid: String,
    val charUuid: String,
)

object BleServices {
    val rpc = BleService("rpcService", BleUuids.SERVICE_UUID, BleUuids.RPC_CHAR_UUID)
    val nearby = BleService("nearbyService", BleUuids.SERVICE_UUID, BleUuids.NEARBY_CHAR_UUID)
}
