package com.ismartcoding.plain.features.dlna.sender

import kotlinx.serialization.Serializable

@Serializable
data class DlnaDevice(val id: String, val hostAddress: String, val name: String) {
    fun getDeviceName(): String = name
}
