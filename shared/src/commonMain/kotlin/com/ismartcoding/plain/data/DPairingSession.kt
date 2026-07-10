package com.ismartcoding.plain.data

import com.ismartcoding.plain.crypto.ECDHKeyPair
import com.ismartcoding.plain.helpers.TimeHelper
import kotlin.time.Instant

data class DPairingSession(
    val deviceId: String,
    val deviceName: String,
    val deviceIp: String,
    val keyPair: ECDHKeyPair,
    val timestamp: Instant = TimeHelper.now(),
)
