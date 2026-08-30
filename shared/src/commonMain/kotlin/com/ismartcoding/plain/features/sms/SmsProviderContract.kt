package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.events.SendResultCodes

object SmsProviderContract {
    const val SEND_RESULT_TIMEOUT = SendResultCodes.TIMEOUT
    const val MMS_PDU_SEND_REQ = 128
    const val MMS_PDU_RETRIEVE_CONF = 132
    const val MMS_CONTENT_FILTER = "m_type IN ($MMS_PDU_SEND_REQ, $MMS_PDU_RETRIEVE_CONF)"

    data class MessageIds(
        val sms: List<String>,
        val mms: List<String>,
    )

    data class SmsSentIntentIdentity(
        val action: String,
        val data: String,
    )

    data class MmsSendFingerprint(
        val address: String,
        val body: String,
        val threadId: String,
        val attachmentContentTypes: List<String>,
    )

    data class MmsCandidateFingerprint(
        val id: Long,
        val address: String,
        val body: String,
        val threadId: String,
        val attachmentContentTypes: List<String>,
    )

    fun partitionMessageIds(value: String): MessageIds {
        val ids = value.split(',').map(String::trim).filter(String::isNotEmpty)
        return MessageIds(
            sms = ids.filterNot { it.startsWith("mms_") },
            mms = ids.filter { it.startsWith("mms_") }.map { it.removePrefix("mms_") },
        )
    }

    fun parseRecipientIds(value: String): List<String> {
        return value.trim()
            .split(Regex("\\s+"))
            .filter(String::isNotEmpty)
            .distinct()
    }

    fun selectConversationAddresses(
        addresses: List<String>,
        ownNumbers: Set<String>,
    ): List<String> {
        val distinctAddresses = addresses.filter(String::isNotBlank).distinctBy(::normalizedAddress)
        val nonSelfAddresses = distinctAddresses.filter { address ->
            ownNumbers.none { ownNumber -> addressesMatch(address, ownNumber, distinctAddresses) }
        }
        return nonSelfAddresses.ifEmpty { distinctAddresses }
    }

    fun addressesMatch(
        first: String,
        second: String,
        candidateContext: Collection<String> = listOf(first),
    ): Boolean {
        val firstAddress = normalizedAddress(first)
        val secondAddress = normalizedAddress(second)
        if (firstAddress == secondAddress) return true
        if (!firstAddress.isPhone || !secondAddress.isPhone || !isPhoneSuffixMatch(firstAddress.value, secondAddress.value)) {
            return false
        }

        val suffixMatches = candidateContext
            .map(::normalizedAddress)
            .filter { it.isPhone && isPhoneSuffixMatch(it.value, secondAddress.value) }
            .distinct()
        return suffixMatches.size == 1 && suffixMatches.single() == firstAddress
    }

    fun smsSentIntentIdentity(packageName: String, requestId: String, partIndex: Int): SmsSentIntentIdentity {
        return SmsSentIntentIdentity(
            action = "$packageName.SMS_SENT.$requestId.$partIndex",
            data = "plainapp://sms/sent/$requestId/$partIndex",
        )
    }

    fun matchingMmsCandidateIds(
        requested: MmsSendFingerprint,
        candidates: List<MmsCandidateFingerprint>,
    ): List<Long> {
        val fingerprintMatches = candidates.filter { candidate ->
            val threadMatches = requested.threadId.isEmpty() || candidate.threadId == requested.threadId
            val bodyMatches = requested.body.isEmpty() || candidate.body.trim() == requested.body.trim()
            val attachmentsMatch = normalizedContentTypes(candidate.attachmentContentTypes) ==
                normalizedContentTypes(requested.attachmentContentTypes)
            threadMatches && bodyMatches && attachmentsMatch
        }
        if (requested.address.isEmpty()) return fingerprintMatches.map { it.id }
        val addressContext = fingerprintMatches.map { it.address }.filter(String::isNotBlank)
        return fingerprintMatches.filter { candidate ->
            addressesMatch(candidate.address, requested.address, addressContext)
        }.map { it.id }
    }

    fun mmsOperationsAreIndistinguishable(
        first: MmsSendFingerprint,
        second: MmsSendFingerprint,
    ): Boolean {
        val bodiesOverlap = first.body.isEmpty() || second.body.isEmpty() || first.body.trim() == second.body.trim()
        val threadsOverlap = first.threadId.isEmpty() || second.threadId.isEmpty() || first.threadId == second.threadId
        return addressesMatch(first.address, second.address) &&
            bodiesOverlap &&
            threadsOverlap &&
            normalizedContentTypes(first.attachmentContentTypes) ==
            normalizedContentTypes(second.attachmentContentTypes)
    }

    fun numericIdPredicate(
        field: String,
        values: Collection<String>,
        chunkSize: Int = 500,
    ): String? {
        require(chunkSize > 0)
        val ids = values.distinct()
        if (ids.isEmpty() || ids.any { id -> id.isEmpty() || id.any { !it.isDigit() } }) return null
        return ids.chunked(chunkSize).joinToString(
            separator = " OR ",
            prefix = "(",
            postfix = ")",
        ) { chunk -> "$field IN (${chunk.joinToString(",")})" }
    }

    fun mmsTextMatches(textParts: List<String>, filters: List<String>): Boolean {
        if (filters.isEmpty()) return true
        val body = textParts.joinToString("\n")
        return filters.all { body.contains(it, ignoreCase = true) }
    }

    private data class NormalizedAddress(val value: String, val isPhone: Boolean)

    private fun normalizedContentTypes(values: List<String>): List<String> {
        return values.map { value ->
            val contentType = value.substringBefore(';').trim().lowercase()
            when {
                contentType.startsWith("image/") -> "image/*"
                contentType.startsWith("video/") -> "video/*"
                contentType.startsWith("audio/") -> "audio/*"
                else -> contentType
            }
        }.sorted()
    }

    private fun normalizedAddress(value: String): NormalizedAddress {
        val trimmed = value.trim()
        val isPhone = trimmed.isNotEmpty() && trimmed.any(Char::isDigit) && trimmed.all {
            it.isDigit() || it.isWhitespace() || it in "+-()./"
        }
        return if (isPhone) {
            NormalizedAddress(trimmed.filter(Char::isDigit), true)
        } else {
            NormalizedAddress(trimmed.lowercase(), false)
        }
    }

    private fun isPhoneSuffixMatch(first: String, second: String): Boolean {
        val shorterLength = minOf(first.length, second.length)
        if (shorterLength < 7 || first.length == second.length) return false
        return first.endsWith(second) || second.endsWith(first)
    }
}
