package com.ismartcoding.plain.data

import com.ismartcoding.plain.lib.crypto.ECDHKeyPair
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

data class DPairingSession(
    val deviceId: String,
    val deviceName: String,
    val deviceIp: String,
    val devicePort: Int,
    val keyPair: ECDHKeyPair,
    val timestamp: Instant = TimeHelper.now(),
)
