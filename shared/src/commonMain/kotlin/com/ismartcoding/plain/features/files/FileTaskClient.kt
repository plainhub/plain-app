package com.ismartcoding.plain.features.files

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.models.FileTask
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object FileTaskClient {
    suspend fun execute(type: FileTaskType, ops: List<FileTaskOp>): FileTask =
        GraphqlApi.execute(Mutation.createFileTask(type, ops))
}
