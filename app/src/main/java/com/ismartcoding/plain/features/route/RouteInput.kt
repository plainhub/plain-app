package com.ismartcoding.plain.features.route

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteInput(
    @SerialName("apply_to") var applyTo: String = "",
    var target: String = "",
    var gateway: String = "",
    @SerialName("if_name") var ifName: String = "",
    @SerialName("is_enabled") var isEnabled: Boolean,
    var notes: String = "",
)
