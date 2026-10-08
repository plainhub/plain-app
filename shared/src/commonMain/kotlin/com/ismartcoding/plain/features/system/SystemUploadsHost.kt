package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object SystemUploadsHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement {
        check(method == "uploadTmpDirFacts")
        return JsonHelper.jsonEncodeToElement(PathFacts(path = com.ismartcoding.plain.platform.getUploadTmpDirPath()))
    }
}
