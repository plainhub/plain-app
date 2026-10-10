package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.data.DUpdateInfo
import kotlinx.serialization.Serializable

@Serializable
data class SystemState(
    val password: String,
    val passwordType: Int,
    val authTwoFactor: Boolean,
    val rotateUrlTokenOnRestart: Boolean,
    val adbToken: String,
    val updateInfo: DUpdateInfo,
    val urlToken: String,
    val apiPermissions: Set<String>,
    val onboardingCompleted: Boolean,
    val clientId: String,
    val mdnsHostname: String,
    val signaturePublicKey: String,
)
