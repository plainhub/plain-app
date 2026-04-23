package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.call.SimHelper
import com.ismartcoding.plain.features.media.CallMediaStoreHelper
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Call
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addCallSchema() {
    query("calls") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver { offset: Int, limit: Int, query: String ->
            Permissions.checkAsync(MainApp.instance, setOf(Permission.READ_CALL_LOG))
            CallMediaStoreHelper.searchAsync(MainApp.instance, query, limit, offset).map { it.toModel() }
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
        resolver { query: String ->
            val context = MainApp.instance
            if (Permission.WRITE_CALL_LOG.enabledAndCanAsync(context)) {
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
        resolver { number: String ->
            Permission.CALL_PHONE.checkAsync(MainApp.instance)
            CallMediaStoreHelper.call(MainActivity.instance.get()!!, number)
            true
        }
    }
    mutation("deleteCalls") {
        resolver { query: String ->
            val context = MainApp.instance
            Permission.WRITE_CALL_LOG.checkAsync(context)
            val newIds = CallMediaStoreHelper.getIdsAsync(context, query)
            TagHelper.deleteTagRelationByKeys(newIds, DataType.CALL)
            CallMediaStoreHelper.deleteByIdsAsync(context, newIds)
            true
        }
    }
}
