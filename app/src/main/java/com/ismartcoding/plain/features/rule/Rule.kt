package com.ismartcoding.plain.features.rule

import kotlinx.serialization.*

@Serializable
data class Rule(
    var id: String = "",
    var action: String = "",
    @SerialName("apply_to") var applyTo: String = "",
    @SerialName("local_port") var localPort: String = "",
    var direction: String = "",
    var protocol: String = "",
    var target: String = "",
    @SerialName("is_enabled") var isEnabled: Boolean = true,
    var notes: String = "",
)
