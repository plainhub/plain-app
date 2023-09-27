package com.ismartcoding.plain.features.route

import com.ismartcoding.plain.data.UIDataCache
import kotlinx.serialization.*

@Serializable
data class Route(
    var id: String = "",
    @SerialName("apply_to") var applyTo: String = "",
    var target: String = "",
    var gateway: String = "",
    @SerialName("if_name") var ifName: String = "",
    @SerialName("is_enabled") var isEnabled: Boolean = true,
    var notes: String = "",
) {
    fun ifDisplayName(): String {
        return UIDataCache.current().getNetworks().find { it.ifName == ifName }?.name ?: ifName
    }
}
