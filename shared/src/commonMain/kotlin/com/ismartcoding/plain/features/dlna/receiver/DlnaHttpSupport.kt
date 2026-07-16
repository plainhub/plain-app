package com.ismartcoding.plain.features.dlna.receiver

/**
 * Pure-Kotlin helpers for the DLNA HTTP server: HTTP response builders,
 * DLNA time parsing, and sender-name resolution.
 *
 * Migrated from androidMain so all business logic lives in commonMain;
 * the platform layer only handles raw socket I/O.
 */

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

internal fun httpOk(body: String, contentType: String = "text/plain"): String {
    val bytes = body.encodeToByteArray()
    return "HTTP/1.1 200 OK\r\nContent-Type: $contentType\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n$body"
}

internal fun httpOkSubscribe(): String {
    return "HTTP/1.1 200 OK\r\nSID: uuid:dlna-plain-sub\r\nTIMEOUT: Second-3600\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
}

internal fun httpNotFound(): String {
    return "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
}

internal fun httpInternalError(): String {
    return "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
}
