package com.ismartcoding.plain.features.telephony.sms

import kotlinx.serialization.Serializable

@Serializable
data class MmsCandidateFacts(val id: Long, val addressCandidates: List<MmsAddressFacts>, val parts: List<MmsPartFacts>)
