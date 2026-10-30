package com.ismartcoding.plain.features.files

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.models.FileTask
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object FileTaskClient : FileTaskApi()

open class FileTaskApi internal constructor(
    protected val api: GraphqlClient = GraphqlApi,
) {
    suspend fun execute(type: FileTaskType, ops: List<FileTaskOp>): FileTask =
        api.execute(Mutation.createFileTask(type, ops))
}
