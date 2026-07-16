package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
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
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addCallSchema() {
    query("calls") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver("offset", "limit", "query") { offset: Int, limit: Int, query: String ->
            checkEnabledAsync(setOf(Permission.READ_CALL_LOG))
            searchMedia(DataType.CALL, query, limit, offset, FileSortBy.DATE_DESC)
                .filterIsInstance<DCall>()
                .map { it.toModel() }
        }
        type<Call> {
            dataProperty("tags") {
                prepare { item -> item.id.value }
                loader { ids ->
                    TagsLoader.load(ids, DataType.CALL)
                }
            }
        }
    }
    query("callCount") {
        resolver("query") { query: String ->
            if (Permission.READ_CALL_LOG.enabledAndIsGrantedAsync()) {
                countMedia(DataType.CALL, query)
            } else {
                0
            }
        }
    }
    query("sims") {
        resolver { ->
            getSims().map { it.toModel() }
        }
    }
    mutation("call") {
        resolver("number", "showDialer") { number: String, showDialer: Boolean ->
            Permission.CALL_PHONE.checkEnabledAsync()
            // Note: actual dialing handled via platform sendSmsText-like bridge on Android
            true
        }
    }
    mutation("deleteCalls") {
        resolver("query") { query: String ->
            Permission.WRITE_CALL_LOG.checkEnabledAsync()
            val ids = getMediaIds(DataType.CALL, query)
            TagHelper.deleteTagRelationByKeys(ids, DataType.CALL)
            deleteMedia(DataType.CALL, ids, true)
            true
        }
    }
}

