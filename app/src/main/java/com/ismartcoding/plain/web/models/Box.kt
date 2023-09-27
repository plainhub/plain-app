package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DBox
import kotlinx.datetime.Instant

data class Box(
    val id: ID,
    val name: String,
    val bluetoothMac: String,
    val ips: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DBox.toModel(): Box {
    return Box(ID(id), name, bluetoothMac, ips, createdAt, updatedAt)
}
