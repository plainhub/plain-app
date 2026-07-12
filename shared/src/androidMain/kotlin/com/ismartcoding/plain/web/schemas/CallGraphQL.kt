package com.ismartcoding.plain.web.schemas
import com.ismartcoding.plain.appContext

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.call.SimHelper
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.media.CallMediaStoreHelper
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
            CallMediaStoreHelper.searchAsync(appContext, query, limit, offset).map { it.toModel() }
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
            val context = appContext
            if (Permission.READ_CALL_LOG.enabledAndIsGrantedAsync()) {
                CallMediaStoreHelper.countAsync(context, query)
            } else {
                0
            }
        }
    }
    query("sims") {
        resolver { ->
            SimHelper.getAll().map { it.toModel() }
        }
    }
    mutation("call") {
        resolver("number", "showDialer") { number: String, showDialer: Boolean ->
            Permission.CALL_PHONE.checkEnabledAsync()
            CallMediaStoreHelper.call(appContext, number, showDialer)
            true
        }
    }
    mutation("deleteCalls") {
        resolver("query") { query: String ->
            val context = appContext
            Permission.WRITE_CALL_LOG.checkEnabledAsync()
            val newIds = CallMediaStoreHelper.getIdsAsync(context, query)
            TagHelper.deleteTagRelationByKeys(newIds, DataType.CALL)
            CallMediaStoreHelper.deleteByIdsAsync(context, newIds)
            true
        }
    }
}
