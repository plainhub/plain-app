package com.ismartcoding.plain.features

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.db.DMediaItem
import com.ismartcoding.plain.enums.DataType
import kotlinx.serialization.json.*

object MediaDurationHelper {
    suspend fun all(): List<DMediaItem> = RustContentApi.query("mediaHostDurations { type mediaId durationMs updatedAt }")
        .getValue("mediaHostDurations").jsonArray.map { value -> value.jsonObject.let {
            DMediaItem(mediaType=it.string("type").lowercase(), mediaId=it.string("mediaId"), durationMs=it.getValue("durationMs").jsonPrimitive.long, updatedAt=it.instant("updatedAt"))
        } }
    suspend fun restore() { all().forEach { TempData.mediaDurationMap.getOrPut("${it.mediaType}:${it.mediaId}") { it.durationMs } } }
    suspend fun save(mediaType: String, mediaId: String, durationMs: Long) {
        val type = DataType.valueOf(mediaType.uppercase())
        RustContentApi.mutate("mediaHostSaveDuration(type: ${type.name}, mediaId: ${gql(mediaId)}, durationMs: $durationMs)")
        TempData.mediaDurationMap["$mediaType:$mediaId"] = durationMs
    }
    suspend fun delete(type: DataType, ids: Collection<String>) {
        RustContentApi.mutate("mediaHostRemoveDurations(type: ${type.name}, mediaIds: ${gqlIds(ids)})")
        ids.forEach { TempData.mediaDurationMap.remove("${type.name.lowercase()}:$it") }
    }
}
