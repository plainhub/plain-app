package com.ismartcoding.plain.api

import com.ismartcoding.plain.data.UIDataCache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object BoxProxyApi {
    private var clients = mutableMapOf<String, OkHttpClient>()

    suspend fun executeAsync(
        body: String,
        timeout: Int,
    ): String {
        val box = UIDataCache.current().box ?: return """{"errors":[{"message":"box_is_null"}]}"""
        val boxApi = box.getBoxIP()
        val client = getOrCreateClient(box.id, box.getBoxIP(), box.token, timeout)
        val request =
            Request.Builder()
                .url("https://$boxApi:8443/graphql")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

        try {
            return client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use """{"errors":[{"message":"[${response.code}]: ${response.message}"}]}"""
                }
                return@use response.body!!.string()
            }
        } catch (ex: Exception) {
            return """{"errors":[{"message":"$ex"}]}"""
        }
    }

    private fun getOrCreateClient(
        boxId: String,
        ip: String,
        token: String,
        timeout: Int,
    ): OkHttpClient {
        val key = "$boxId:$ip:$timeout"
        var client = clients[key]
        if (client == null) {
            client = HttpClientManager.createCryptoHttpClient(token, timeout)
            clients[key] = client
        }

        return client
    }
}
