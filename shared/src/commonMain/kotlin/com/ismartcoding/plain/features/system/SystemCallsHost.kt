package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.data.DCall
import com.ismartcoding.plain.data.getGeo
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object SystemCallsHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemCallFacts" -> {
            val query = params.getValue("query").jsonPrimitive.content
            val offset = params.getValue("offset").jsonPrimitive.int
            val limit = params.getValue("limit").jsonPrimitive.int
            JsonHelper.jsonEncodeToElement(
                com.ismartcoding.plain.platform.searchMedia(
                    com.ismartcoding.plain.enums.DataType.CALL, query, limit, offset,
                    com.ismartcoding.plain.features.file.FileSortBy.DATE_DESC,
                ).filterIsInstance<DCall>().map(::callFacts)
            )
        }
        "systemCallCount" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.countMedia(
                com.ismartcoding.plain.enums.DataType.CALL,
                params.getValue("query").jsonPrimitive.content,
            )
        )
        "systemCallIds" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getMediaIds(
                com.ismartcoding.plain.enums.DataType.CALL,
                params.getValue("query").jsonPrimitive.content,
            )
        )
        "systemMakeCall" -> {
            com.ismartcoding.plain.platform.call(
                params.getValue("number").jsonPrimitive.content,
                params.getValue("showDialer").jsonPrimitive.boolean,
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        else -> error("Unsupported provider operation")
    }

    /** The public call-log contract; the geo lookup stays on the platform. */
    private fun callFacts(call: DCall): CallFacts {
        val geo = call.getGeo()
        return CallFacts(
            id = call.id,
            number = call.number,
            name = call.name,
            photoId = com.ismartcoding.plain.helpers.getFileId(call.photoUri),
            startedAt = call.startedAt.toString(),
            durationSec = call.durationSec,
            type = call.type,
            accountId = call.accountId,
            geo = geo?.let { value ->
                PhoneGeoFacts(
                    country = value.country,
                    numberType = value.numberType,
                    carrier = value.carrier,
                    description = value.description,
                )
            },
        )
    }
}
