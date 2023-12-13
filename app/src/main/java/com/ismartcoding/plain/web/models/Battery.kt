package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DBattery

class Battery {
    var level: Int = -1
    var voltage: Int = 0
    var health: Int = -1
    var plugged: Int = -1
    var temperature: Int = 0
    var status: Int = -1
    var technology: String = ""
    var capacity: Int = 0
}

fun DBattery.toModel(): Battery {
    val model = Battery()
    model.level = this.level
    model.voltage = this.voltage
    model.health = this.health
    model.plugged = this.plugged
    model.temperature = this.temperature
    model.status = this.status
    model.technology = this.technology
    model.capacity = this.capacity

    return model
}