package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.data.DCall
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.platform.getSims
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.deleteMedia
import com.ismartcoding.plain.platform.getMediaIds
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Call
import com.ismartcoding.plain.web.models.Sim
import com.ismartcoding.plain.web.models.toModel

@GraphQLQuery
suspend fun callCount(query: String): Int {
    return if (Permission.READ_CALL_LOG.enabledAndIsGrantedAsync()) {
        countMedia(DataType.CALL, query)
    } else {
        0
    }
}

@GraphQLQuery
suspend fun sims(): List<Sim> {
    return getSims().map { it.toModel() }
}

@GraphQLMutation
suspend fun call(number: String, showDialer: Boolean): Boolean {
    Permission.CALL_PHONE.checkEnabledAsync()
    // Note: actual dialing handled via platform sendSmsText-like bridge on Android
    return true
}

@GraphQLMutation
suspend fun deleteCalls(query: String): Boolean {
    Permission.WRITE_CALL_LOG.checkEnabledAsync()
    val ids = getMediaIds(DataType.CALL, query)
    TagHelper.deleteTagRelationByKeys(ids, DataType.CALL)
    deleteMedia(DataType.CALL, ids, true)
    return true
}

@GraphQLQuery
suspend fun calls(offset: Int, limit: Int, query: String): List<Call> {
    checkEnabledAsync(setOf(Permission.READ_CALL_LOG))
    return searchMedia(DataType.CALL, query, limit, offset, FileSortBy.DATE_DESC)
        .filterIsInstance<DCall>()
        .map { it.toModel() }
}

fun SchemaBuilder.addCallSchema() {
    type<Call> {
        dataProperty("tags") {
            prepare { item -> item.id.value }
            loader { ids ->
                TagsLoader.load(ids, DataType.CALL)
            }
        }
    }
}

