package com.ismartcoding.plain.data

import com.ismartcoding.plain.helpers.TimeHelper
import java.security.KeyPair
import kotlin.time.Instant

data class DPairingSession(
    val deviceId: String,
    val deviceName: String,
    val deviceIp: String,
    val keyPair: KeyPair,
    val timestamp: Instant = TimeHelper.now()
)
