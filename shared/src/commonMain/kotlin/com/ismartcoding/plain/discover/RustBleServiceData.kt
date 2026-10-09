package com.ismartcoding.plain.discover

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject

object RustBleServiceData {
    private suspend inline fun <reified T, reified R> call(body: T): R {
        val response = RustContentApi.postJsonOrThrow("chat/discovery", JsonHelper.jsonEncodeToElement(body).jsonObject)
        return JsonHelper.jsonDecodeFromElement(response.getValue("result"))
    }

    suspend fun decode(data: ByteArray?): BleAdvertisement? =
        call(DecodeRequest(payload = data?.map { it.toInt() and 255 }))

    suspend fun shortIdOf(clientId: String): String = call(ShortIdRequest(id = clientId))

    @Serializable
    private data class DecodeRequest(val action: String = "bleDecode", val payload: List<Int>?)

    @Serializable
    private data class ShortIdRequest(val action: String = "bleShortId", val id: String)
}
