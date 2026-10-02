package com.ismartcoding.plain.ble

object BleServices {
    val http = BleService("httpService", BleUuids.SERVICE_UUID, BleUuids.HTTP_CHAR_UUID)
    val nearby = BleService("nearbyService", BleUuids.SERVICE_UUID, BleUuids.NEARBY_CHAR_UUID)
}
