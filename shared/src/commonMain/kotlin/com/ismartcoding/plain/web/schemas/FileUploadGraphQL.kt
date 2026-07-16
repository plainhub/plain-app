package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.deleteUploadedChunks
import com.ismartcoding.plain.platform.listUploadedChunks
import com.ismartcoding.plain.platform.mergeUploadedChunks

fun SchemaBuilder.addFileUploadSchema() {
    query("uploadedChunks") {
        resolver("fileId") { fileId: String ->
            listUploadedChunks(fileId)
        }
    }
    mutation("deleteChunks") {
        resolver("fileId") { fileId: String ->
            deleteUploadedChunks(fileId)
        }
    }
    mutation("mergeChunks") {
        resolver("fileId", "totalChunks", "path", "replace", "isAppFile") { fileId: String, totalChunks: Int, path: String, replace: Boolean, isAppFile: Boolean ->
            mergeUploadedChunks(fileId, totalChunks, path, replace, isAppFile)
        }
    }
}
