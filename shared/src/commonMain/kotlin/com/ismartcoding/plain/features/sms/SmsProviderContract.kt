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
