package com.ismartcoding.plain.features.dlna.receiver

/**
 * Pure-Kotlin helpers for the DLNA receiver HTTP routes: structured response
 * type, response builders, DLNA time parsing, and sender-name resolution.
 *
 * The receiver HTTP endpoints are served by the shared web server (see
 * `web/routes/DlnaRoutes.kt`); these helpers produce [DlnaHttpResponse]
 * values that the route handler applies to the platform-agnostic [HttpCall].
 */
data class DlnaHttpResponse(
    val status: Int,
    val contentType: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
)

internal fun resolveSenderName(headers: Map<String, String>, senderIp: String): String {
    return headers["c-name"]?.takeIf { it.isNotBlank() } ?: senderIp
}

/**
 * Parses a DLNA time string (`HH:MM:SS` or `HH:MM:SS.mmm`) to milliseconds.
 * @return the duration in milliseconds, or -1 if the string is malformed.
 */
internal fun parseDlnaTimeToMs(time: String): Long {
    val parts = time.split(":")
    return if (parts.size >= 3) {
        val h = parts[0].toLongOrNull() ?: return -1L
        val m = parts[1].toLongOrNull() ?: return -1L
        val s = parts[2].split(".")[0].toLongOrNull() ?: return -1L
        (h * 3600 + m * 60 + s) * 1000
    } else -1L
}

internal fun httpOk(body: String, contentType: String = "text/plain"): DlnaHttpResponse =
    DlnaHttpResponse(status = 200, contentType = contentType, body = body)

internal fun httpOkSubscribe(): DlnaHttpResponse =
    DlnaHttpResponse(
        status = 200,
        headers = mapOf("SID" to "uuid:dlna-plain-sub", "TIMEOUT" to "Second-3600"),
    )

internal fun httpNotFound(): DlnaHttpResponse = DlnaHttpResponse(status = 404)

internal fun httpInternalError(): DlnaHttpResponse = DlnaHttpResponse(status = 500)
