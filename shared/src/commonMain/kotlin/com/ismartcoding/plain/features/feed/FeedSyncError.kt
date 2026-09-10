package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.platform.NetworkType

/** Classified reasons a feed sync can fail, persisted on DFeed and shown in the UI. */
enum class FeedSyncErrorCode {
    NONE,
    NO_NETWORK,
    DNS,
    TIMEOUT,
    SERVER,
    SSL,
    PARSE,
    UNKNOWN;

    companion object {
        /** Null-safe parse of the DFeed.lastError.code column; "" and garbage map to null. */
        fun fromString(code: String): FeedSyncErrorCode? =
            if (code.isEmpty()) null else entries.firstOrNull { it.name == code }
    }
}

/**
 * Maps a sync Throwable to a [FeedSyncErrorCode]. Runs in commonMain and
 * matches on exception class names plus message patterns so both platform
 * backends are covered without expect/actual:
 * - Android/OkHttp throws java.net.* exceptions (UnknownHostException, ...).
 * - iOS/NSURLSession failures are wrapped by PlainHttpClient into
 *   Exception("NSURLErrorDomain <code> <localizedDescription>").
 * - fetchRssChannel throws Exception("HTTP <status>") for non-200 responses.
 */
object FeedSyncErrorClassifier {
    fun classify(ex: Throwable, networkType: NetworkType): FeedSyncErrorCode {
        if (networkType == NetworkType.NONE) return FeedSyncErrorCode.NO_NETWORK
        var t: Throwable? = ex
        while (t != null) {
            val name = t::class.simpleName ?: ""
            val message = t.message ?: ""
            if (message.startsWith("HTTP ")) return FeedSyncErrorCode.SERVER
            if (message.contains("NSURLErrorDomain")) {
                return when {
                    message.contains("-1009") -> FeedSyncErrorCode.NO_NETWORK
                    message.contains("-1003") -> FeedSyncErrorCode.DNS
                    message.contains("-1001") || message.contains("-1004") -> FeedSyncErrorCode.TIMEOUT
                    Regex("-(12[0-9][0-9])").containsMatchIn(message) -> FeedSyncErrorCode.SSL
                    else -> FeedSyncErrorCode.UNKNOWN
                }
            }
            when {
                name == "UnknownHostException" -> return FeedSyncErrorCode.DNS
                name == "SocketTimeoutException" || name == "TimeoutCancellationException" ||
                    name == "ConnectException" || name == "TimeoutException" -> return FeedSyncErrorCode.TIMEOUT
                name.startsWith("SSL") || name == "CertificateException" ||
                    name == "CertPathValidatorException" -> return FeedSyncErrorCode.SSL
                name == "IllegalArgumentException" -> return FeedSyncErrorCode.PARSE
                name == "OpmlParseException" -> return FeedSyncErrorCode.PARSE
                message.contains("timed out", ignoreCase = true) ||
                    message.contains("timeout", ignoreCase = true) -> return FeedSyncErrorCode.TIMEOUT
            }
            t = t.cause
        }
        return FeedSyncErrorCode.UNKNOWN
    }
}
