package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.settings.PasswordType

internal data class PasswordSettings(
    val passwordConfigured: Boolean,
    val passwordType: PasswordType,
)
