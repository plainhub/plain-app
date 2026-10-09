package com.ismartcoding.plain.ble

data class BleService(
    val name: String,
    val serviceUuid: String,
    val charUuid: String,
)
