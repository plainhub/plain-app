package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.ai.ImageIndexManager
import com.ismartcoding.plain.ai.ImageSearchIndexer
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.events.CancelImageDownloadEvent
import com.ismartcoding.plain.events.DisableImageSearchEvent
import com.ismartcoding.plain.events.EnableImageSearchEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.media.ImageSearchHelper
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Image
import com.ismartcoding.plain.web.models.ImageSearchStatus
import com.ismartcoding.plain.web.models.buildImageSearchStatus
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addImageSchema() {
    query("images") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            val context = MainApp.instance
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
            val fields = SearchHelper.parse(query)
            val textField = fields.find { it.name == "text" }
            val queryText = textField?.value ?: ""
            ImageSearchHelper.searchCombinedAsync(
                context = context,
                queryText = queryText,
                extraQuery = query,
                limit = limit,
                offset = offset,
                sortBy = sortBy
            ).map { it.toModel() }
        }
        type<Image> {
            dataProperty("tags") {
                prepare { item -> item.id.value }
                loader { ids ->
                    TagsLoader.load(ids, DataType.IMAGE)
                }
            }
        }
    }
    query("imageCount") {
        resolver { query: String ->
            val context = MainApp.instance
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndCanAsync(context)) {
                val fields = SearchHelper.parse(query)
                val textField = fields.find { it.name == "text" }
                val queryText = textField?.value ?: ""
                ImageSearchHelper.countCombinedAsync(
                    context = context,
                    queryText = queryText,
                    extraQuery = query
                )
            } else {
                0
            }
        }
    }
    query("imageSearchStatus") {
        resolver { -> buildImageSearchStatus() }
    }
    type<ImageSearchStatus> {}
    mutation("enableImageSearch") {
        resolver { ->
            sendEvent(EnableImageSearchEvent())
            true
        }
    }
    mutation("disableImageSearch") {
        resolver { ->
            sendEvent(DisableImageSearchEvent())
            true
        }
    }
    mutation("cancelImageDownload") {
        resolver { ->
            sendEvent(CancelImageDownloadEvent())
            true
        }
    }
    mutation("startImageIndex") {
        resolver { force: Boolean? ->
            ImageIndexManager.fullScan(force == true)
            true
        }
    }
    mutation("cancelImageIndex") {
        resolver { ->
            ImageSearchIndexer.cancel()
            true
        }
    }
}
