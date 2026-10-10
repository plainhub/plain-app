package com.ismartcoding.plain.api

class ContentApiSession(val baseUrl: String, val deviceId: String, private val bearerToken: String, private val clientId: String? = null) {
    internal fun headers(): Map<String, String> = mapOf("Authorization" to "Bearer $bearerToken") +
        (clientId?.let { mapOf("c-id" to it) } ?: emptyMap())
}
