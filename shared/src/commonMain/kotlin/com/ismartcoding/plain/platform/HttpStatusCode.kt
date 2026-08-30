package com.ismartcoding.plain.platform

import kotlin.jvm.JvmInline

/**
 * HTTP status code with its standard reason phrase, so call sites can compare
 * against named codes (`response.status == HttpStatusCode.Forbidden`) and still
 * format human-readable errors (`${status.value} ${status.description}`).
 */
@JvmInline
value class HttpStatusCode(val value: Int) {
    val description: String get() = REASON_PHRASES[value] ?: ""

    val isSuccess: Boolean get() = value in 200..299

    override fun toString(): String = if (description.isEmpty()) "$value" else "$value $description"

    companion object {
        val OK = HttpStatusCode(200)
        val Created = HttpStatusCode(201)
        val Accepted = HttpStatusCode(202)
        val NoContent = HttpStatusCode(204)
        val Found = HttpStatusCode(302)
        val NotModified = HttpStatusCode(304)
        val BadRequest = HttpStatusCode(400)
        val Unauthorized = HttpStatusCode(401)
        val Forbidden = HttpStatusCode(403)
        val NotFound = HttpStatusCode(404)
        val MethodNotAllowed = HttpStatusCode(405)
        val RequestTimeout = HttpStatusCode(408)
        val Conflict = HttpStatusCode(409)
        val TooManyRequests = HttpStatusCode(429)
        val InternalServerError = HttpStatusCode(500)
        val BadGateway = HttpStatusCode(502)
        val ServiceUnavailable = HttpStatusCode(503)
        val GatewayTimeout = HttpStatusCode(504)

        private val REASON_PHRASES = mapOf(
            200 to "OK",
            201 to "Created",
            202 to "Accepted",
            204 to "No Content",
            206 to "Partial Content",
            301 to "Moved Permanently",
            302 to "Found",
            304 to "Not Modified",
            400 to "Bad Request",
            401 to "Unauthorized",
            403 to "Forbidden",
            404 to "Not Found",
            405 to "Method Not Allowed",
            408 to "Request Timeout",
            409 to "Conflict",
            429 to "Too Many Requests",
            500 to "Internal Server Error",
            502 to "Bad Gateway",
            503 to "Service Unavailable",
            504 to "Gateway Timeout",
        )
    }
}
