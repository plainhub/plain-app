package com.ismartcoding.plain.data

import kotlin.time.Instant

@kotlinx.serialization.Serializable
data class DCertificate(val issuer: String, val subject: String, val serialNumber: String, val validFrom: Instant, val validTo: Instant)
