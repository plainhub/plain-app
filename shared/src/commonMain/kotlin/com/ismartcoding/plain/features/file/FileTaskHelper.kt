package com.ismartcoding.plain.features.file

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.preferences.SystemPrefs
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlin.time.Instant

object FileTaskHelper {
    private const val fields = "id type status title error totalBytes doneBytes totalItems doneItems createdAt updatedAt completedOps { src dst }"
    private val clientId: String get() = SystemPrefs.clientId.value.also { check(it.isNotEmpty()) { "File task identity unavailable" } }
    suspend fun create(type: FileTaskType, ops: List<FileTaskOp>, title: String = type.name): DFileTask {
        val inputs = ops.joinToString(",") { "{ src: ${gql(it.src)}, dst: ${gql(it.dst)}, overwrite: ${it.overwrite} }" }
        return parse(RustContentApi.mutate("createFileHostTask(clientId: ${gql(clientId)}, type: ${type.name}, title: ${gql(title)}, ops: [$inputs]) { $fields }").getValue("createFileHostTask"))
    }
    suspend fun get(id: String): DFileTask? = RustContentApi.query("fileHostTaskRecord(clientId: ${gql(clientId)}, id: ${gql(id)}) { $fields }").getValue("fileHostTaskRecord").takeUnless { it is JsonNull }?.let(::parse)
    suspend fun wait(id: String): DFileTask {
        while (true) {
            currentCoroutineContext().ensureActive()
            val task = checkNotNull(get(id)) { "File task unavailable" }
            if (task.status == FileTaskStatus.DONE || task.status == FileTaskStatus.ERROR) return task
            delay(100)
        }
    }
    suspend fun execute(type: FileTaskType, ops: List<FileTaskOp>): DFileTask = wait(create(type,ops).id)
    suspend fun remove(id: String): Boolean = RustContentApi.mutate("removeFileHostTask(clientId: ${gql(clientId)}, id: ${gql(id)})").getValue("removeFileHostTask").jsonPrimitive.boolean
    suspend fun list(offset: Int, limit: Int, query: String): List<DFileTask> = RustContentApi.query("fileHostTasks(clientId: ${gql(clientId)}, offset: $offset, limit: $limit, query: ${gql(query)}) { $fields }").getValue("fileHostTasks").jsonArray.map(::parse)
    private fun parse(value: JsonElement): DFileTask = value.jsonObject.let { row ->
        DFileTask(row.string("id"),FileTaskType.valueOf(row.string("type")),FileTaskStatus.valueOf(row.string("status")),row.string("title"),row.string("error"),row.getValue("totalBytes").jsonPrimitive.long,row.getValue("doneBytes").jsonPrimitive.long,row.getValue("totalItems").jsonPrimitive.long,row.getValue("doneItems").jsonPrimitive.long,Instant.parse(row.string("createdAt")),Instant.parse(row.string("updatedAt")),row.getValue("completedOps").jsonArray.map { it.jsonObject.let { op -> FileTaskCompletedOp(op.string("src"),op.string("dst")) } })
    }
}
