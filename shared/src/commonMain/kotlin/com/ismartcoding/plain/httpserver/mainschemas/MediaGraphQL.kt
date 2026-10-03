package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.MediaDataType
import com.ismartcoding.plain.enums.toDataType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.platform.getMediaBuckets
import com.ismartcoding.plain.platform.getMediaIds
import com.ismartcoding.plain.platform.getTrashedMediaIds
import com.ismartcoding.plain.helpers.FilePathValidator
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.httpserver.models.ActionResult
import com.ismartcoding.plain.httpserver.models.MediaBucket
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun mediaBuckets(type: MediaDataType): List<MediaBucket> {
    return if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
        getMediaBuckets(type.toDataType()).map { it.toModel() }
    } else {
        emptyList()
    }
}

@GraphQLMutation
suspend fun deleteMediaItems(type: MediaDataType, query: String): ActionResult {
    QueryHelper.requireExplicitBulkQuery(query)
    val dataType = type.toDataType()
    val fromTrash = AppFeatureType.MEDIA_TRASH.has()
    val ids = if (fromTrash) getTrashedMediaIds(dataType,query) else getMediaIds(dataType,query)
    return ActionResult(com.ismartcoding.plain.features.mediaactions.MediaActionHelper.run(dataType,com.ismartcoding.plain.features.mediaactions.MediaAction.DELETE,ids,fromTrash))
}

@GraphQLMutation
suspend fun trashMediaItems(type: MediaDataType, query: String): ActionResult {
    QueryHelper.requireExplicitBulkQuery(query)
    val dataType = type.toDataType()
    return ActionResult(com.ismartcoding.plain.features.mediaactions.MediaActionHelper.run(dataType,com.ismartcoding.plain.features.mediaactions.MediaAction.TRASH,getMediaIds(dataType,query)))
}

@GraphQLMutation
suspend fun restoreMediaItems(type: MediaDataType, query: String): ActionResult {
    QueryHelper.requireExplicitBulkQuery(query)
    val dataType = type.toDataType()
    return ActionResult(com.ismartcoding.plain.features.mediaactions.MediaActionHelper.run(dataType,com.ismartcoding.plain.features.mediaactions.MediaAction.RESTORE,getTrashedMediaIds(dataType,query)))
}

@GraphQLMutation
suspend fun moveMediaItems(type: MediaDataType, query: String, destDir: String): ActionResult {
    QueryHelper.requireExplicitBulkQuery(query)
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    FilePathValidator.requireAllSafe(listOf(destDir))
    val dataType = type.toDataType()
    return ActionResult(com.ismartcoding.plain.features.mediaactions.MediaActionHelper.run(dataType,com.ismartcoding.plain.features.mediaactions.MediaAction.MOVE,getMediaIds(dataType,query),destDir=destDir))
}

fun SchemaBuilder.addMediaSchema() {
}
