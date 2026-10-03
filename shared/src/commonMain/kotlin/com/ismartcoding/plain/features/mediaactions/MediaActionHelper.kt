package com.ismartcoding.plain.features.mediaactions

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.DataType
import kotlinx.serialization.json.*

object MediaActionHelper {
    suspend fun run(type: DataType, action: MediaAction, ids: Collection<String>, fromTrash: Boolean = false, destDir: String = ""): Int {
        var affected = 0
        val failed = mutableListOf<String>()
        for (batch in ids.distinct().chunked(500)) {
            val value = RustContentApi.mutate("mediaHostAction(type: ${type.name}, action: ${action.name}, ids: ${gqlIds(batch)}, fromTrash: $fromTrash, destDir: ${gql(destDir)}) { affectedCount failedIds }").getValue("mediaHostAction").jsonObject
            affected += value.getValue("affectedCount").jsonPrimitive.int
            val batchFailed = value.getValue("failedIds").jsonArray.map { it.jsonPrimitive.content }.toSet()
            failed.addAll(batchFailed)
            if (action == MediaAction.TRASH || action == MediaAction.DELETE) {
                batch.filterNot { it in batchFailed }.forEach { id ->
                    TempData.mediaDurationMap.remove("${type.name.lowercase()}:$id")
                    if (type == DataType.VIDEO) TempData.videoPlayProgressMap.remove(id)
                }
            }
        }
        check(failed.isEmpty()) { "${action.name}: ${failed.size} media actions failed ($affected completed)" }
        return affected
    }
}
