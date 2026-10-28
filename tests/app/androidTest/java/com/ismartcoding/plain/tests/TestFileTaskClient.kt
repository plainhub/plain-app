package com.ismartcoding.plain.tests

import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlApi

import com.ismartcoding.plain.features.files.FileTaskType
import com.ismartcoding.plain.features.files.FileTaskOp

import com.ismartcoding.plain.models.FileTask
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object TestFileTaskClient {
    private val json = Json { encodeDefaults = true }
    private const val fields = "id clientId type status title error totalBytes doneBytes totalItems doneItems createdAt updatedAt completedOps { src dst }"

    suspend fun create(type: FileTaskType, ops: List<FileTaskOp>, title: String = type.name): FileTask = submit(type, ops, title, false)

    private suspend fun submit(type: FileTaskType, ops: List<FileTaskOp>, title: String, wait: Boolean): FileTask =
        parse(GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation(\$type:FileTaskType!,\$title:String!,\$ops:[FileTaskOpInput!]!,\$wait:Boolean!){createFileTask(type:\$type,title:\$title,ops:\$ops,waitForCompletion:\$wait){ $fields }}", mapOf("type" to JsonPrimitive(type.name), "title" to JsonPrimitive(title), "ops" to json.encodeToJsonElement(ops), "wait" to JsonPrimitive(wait)))).getValue("createFileTask"))

    suspend fun list(offset: Int, limit: Int, query: String): List<FileTask> = GraphqlApi.execute(GraphqlRequest<JsonObject>("query { fileTasks(offset: $offset, limit: $limit, query: ${JsonHelper.jsonEncode(query)}) { $fields } }")).getValue("fileTasks").jsonArray.map(::parse)
    private fun parse(value: JsonElement): FileTask = JsonHelper.jsonDecodeFromElement(value)
}
