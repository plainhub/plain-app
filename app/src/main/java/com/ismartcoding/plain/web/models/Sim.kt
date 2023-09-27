package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.call.DSim

data class Sim(val id: ID, val label: String, val number: String)

fun DSim.toModel(): Sim {
    return Sim(ID(id), label, number)
}
