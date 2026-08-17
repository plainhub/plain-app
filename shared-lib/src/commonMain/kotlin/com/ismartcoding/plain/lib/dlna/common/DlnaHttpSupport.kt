package com.ismartcoding.plain.lib.dlna.common

/**
 * Pure-Kotlin helpers for the DLNA receiver HTTP routes: structured response
 * type, response builders, DLNA time parsing, and sender-name resolution.
 */
data class DlnaHttpResponse(
    val status: Int,
    val contentType: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
)

fun resolveSenderName(headers: Map<String, String>, senderIp: String): String {
    return headers["c-name"]?.takeIf { it.isNotBlank() } ?: senderIp
}

/**
 * Parses a DLNA time string (`HH:MM:SS` or `HH:MM:SS.mmm`) to milliseconds.
 * @return the duration in milliseconds, or -1 if the string is malformed.
 */
fun parseDlnaTimeToMs(time: String): Long {
    val parts = time.split(":")
    return if (parts.size >= 3) {
        val h = parts[0].toLongOrNull() ?: return -1L
        val m = parts[1].toLongOrNull() ?: return -1L
        val s = parts[2].split(".")[0].toLongOrNull() ?: return -1L
        (h * 3600 + m * 60 + s) * 1000
    } else -1L
}

fun httpOk(body: String, contentType: String = "text/plain"): DlnaHttpResponse =
    DlnaHttpResponse(status = 200, contentType = contentType, body = body)

fun httpOkSubscribe(): DlnaHttpResponse =
    DlnaHttpResponse(
        status = 200,
        headers = mapOf("SID" to "uuid:dlna-plain-sub", "TIMEOUT" to "Second-3600"),
    )

fun httpNotFound(): DlnaHttpResponse = DlnaHttpResponse(status = 404)

fun httpInternalError(): DlnaHttpResponse = DlnaHttpResponse(status = 500)
