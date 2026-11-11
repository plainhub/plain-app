package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.settings.PasswordType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PasswordSettings(
    val passwordConfigured: Boolean,
    @SerialName("password_type") val passwordType: PasswordType,
)
