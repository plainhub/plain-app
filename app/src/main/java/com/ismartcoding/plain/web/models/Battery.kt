package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DBattery

enum class BatteryHealth {
    UNKNOWN, GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, UNSPECIFIED_FAILURE, COLD
}

enum class BatteryStatus {
    UNKNOWN, CHARGING, DISCHARGING, NOT_CHARGING, FULL
}

enum class BatteryPlugged {
    UNPLUGGED, AC, USB, WIRELESS
}

class Battery {
    var level: Int = -1
    var voltage: Int = 0
    var health: BatteryHealth = BatteryHealth.UNKNOWN
    var plugged: BatteryPlugged = BatteryPlugged.UNPLUGGED
    var temperature: Int = 0
    var status: BatteryStatus = BatteryStatus.UNKNOWN
    var technology: String = ""
    var capacity: Int = 0
}

fun DBattery.toModel(): Battery {
    val model = Battery()
    model.level = this.level
    model.voltage = this.voltage
    model.health = when (this.health) {
        2 -> BatteryHealth.GOOD
        3 -> BatteryHealth.OVERHEAT
        4 -> BatteryHealth.DEAD
        5 -> BatteryHealth.OVER_VOLTAGE
        6 -> BatteryHealth.UNSPECIFIED_FAILURE
        7 -> BatteryHealth.COLD
        else -> BatteryHealth.UNKNOWN
    }
    model.status = when (this.status) {
        2 -> BatteryStatus.CHARGING
        3 -> BatteryStatus.DISCHARGING
        4 -> BatteryStatus.NOT_CHARGING
        5 -> BatteryStatus.FULL
        else -> BatteryStatus.UNKNOWN
    }
    model.plugged = when (this.plugged) {
        1 -> BatteryPlugged.AC
        2 -> BatteryPlugged.USB
        4 -> BatteryPlugged.WIRELESS
        else -> BatteryPlugged.UNPLUGGED
    }
    model.temperature = this.temperature
    model.technology = this.technology
    model.capacity = this.capacity
    return model
}