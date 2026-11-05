package com.ismartcoding.plain.platform.network.ble

object BleServices {
    val http = BleService("httpService", BleUuids.HTTP_CHAR_UUID)
    val nearby = BleService("nearbyService", BleUuids.NEARBY_CHAR_UUID)
}
