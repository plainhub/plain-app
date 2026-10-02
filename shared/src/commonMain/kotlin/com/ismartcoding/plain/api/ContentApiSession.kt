package com.ismartcoding.plain.api

class ContentApiSession(val baseUrl: String, val deviceId: String, private val bearerToken: String) {
    internal fun headers(): Map<String, String> = mapOf("Authorization" to "Bearer $bearerToken")
}
