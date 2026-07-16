package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.SearchHelper
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.events.HCancelImageModelDownloadEvent
import com.ismartcoding.plain.events.HDisableImageSearchEvent
import com.ismartcoding.plain.events.HEnableImageSearchEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.buildImageSearchStatus
import com.ismartcoding.plain.platform.cancelImageIndex
import com.ismartcoding.plain.platform.countImagesCombined
import com.ismartcoding.plain.platform.enqueueRemoveImageIndex
import com.ismartcoding.plain.platform.searchImagesCombined
import com.ismartcoding.plain.platform.startImageIndexFullScan
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Image
import com.ismartcoding.plain.web.models.ImageSearchStatus
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addImageSchema() {
    query("images") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver("offset", "limit", "query", "sortBy") { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            val fields = SearchHelper.parse(query)
            val textField = fields.find { it.name == "text" }
            val queryText = textField?.value ?: ""
            searchImagesCombined(
                queryText = queryText,
                extraQuery = query,
                limit = limit,
                offset = offset,
                sortBy = sortBy,
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
        resolver("query") { query: String ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
                val fields = SearchHelper.parse(query)
                val textField = fields.find { it.name == "text" }
                val queryText = textField?.value ?: ""
                countImagesCombined(
                    queryText = queryText,
                    extraQuery = query,
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
            sendEvent(HEnableImageSearchEvent())
            true
        }
    }
    mutation("disableImageSearch") {
        resolver { ->
            sendEvent(HDisableImageSearchEvent())
            true
        }
    }
    mutation("cancelImageModelDownload") {
        resolver { ->
            sendEvent(HCancelImageModelDownloadEvent())
            true
        }
    }
    mutation("startImageIndex") {
        resolver("force") { force: Boolean? ->
            startImageIndexFullScan(force == true)
            true
        }
    }
    mutation("cancelImageIndex") {
        resolver { ->
            cancelImageIndex()
            true
        }
    }
}
