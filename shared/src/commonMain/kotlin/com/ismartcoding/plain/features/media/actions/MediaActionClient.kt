package com.ismartcoding.plain.features.media.actions

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.selectionQuery

object MediaActionClient : MediaActionApi()

open class MediaActionApi internal constructor(protected val api: GraphqlClient = GraphqlApi) {
    suspend fun run(
        type: DataType,
        action: MediaAction,
        ids: Collection<String>,
        fromTrash: Boolean = false,
        destDir: String = "",
    ): Int {
        require(type in setOf(DataType.AUDIO, DataType.VIDEO, DataType.IMAGE, DataType.DOC)) { "Unsupported media type" }
        if (ids.isEmpty()) return 0
        val query = mediaActionSelection(ids, fromTrash || action == MediaAction.RESTORE)
        val request = Mutation.mediaAction(action, type, query, destDir)
        return api.execute(request).affectedCount
    }
}
internal fun mediaActionSelection(ids: Collection<String>, fromTrash: Boolean): String {
    return "${selectionQuery(ids)} trash:$fromTrash"
}
