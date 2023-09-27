package com.ismartcoding.plain.features.rule

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RuleInput(
    var action: String = "",
    @SerialName("apply_to") var applyTo: String = "",
    @SerialName("local_port") var localPort: String = "",
    var direction: String = "",
    var protocol: String = "",
    var target: String = "",
    @SerialName("is_enabled") var isEnabled: Boolean = true,
    var notes: String = "",
)
