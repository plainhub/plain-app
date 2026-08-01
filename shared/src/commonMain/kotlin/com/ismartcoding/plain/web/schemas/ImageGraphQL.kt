package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.helpers.SearchHelper
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.events.HCancelImageModelDownloadEvent
import com.ismartcoding.plain.events.HDisableImageSearchEvent
import com.ismartcoding.plain.events.HEnableImageSearchEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.buildImageSearchStatus
import com.ismartcoding.plain.platform.cancelImageIndex
import com.ismartcoding.plain.platform.countImagesCombined
import com.ismartcoding.plain.platform.searchImagesCombined
import com.ismartcoding.plain.platform.startImageIndexFullScan
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Image
import com.ismartcoding.plain.web.models.ImageSearchStatus
import com.ismartcoding.plain.web.models.toModel

@GraphQLQuery
suspend fun imageCount(query: String): Int {
    return if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
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

@GraphQLQuery
suspend fun imageSearchStatus(): ImageSearchStatus {
    return buildImageSearchStatus()
}

@GraphQLMutation
suspend fun enableImageSearch(): Boolean {
    sendEvent(HEnableImageSearchEvent())
    return true
}

@GraphQLMutation
suspend fun disableImageSearch(): Boolean {
    sendEvent(HDisableImageSearchEvent())
    return true
}

@GraphQLMutation
suspend fun cancelImageModelDownload(): Boolean {
    sendEvent(HCancelImageModelDownloadEvent())
    return true
}

@GraphQLMutation
suspend fun startImageIndex(force: Boolean?): Boolean {
    startImageIndexFullScan(force == true)
    return true
}

@GraphQLMutation
suspend fun cancelImageIndex(): Boolean {
    cancelImageIndex()
    return true
}

@GraphQLQuery
suspend fun images(offset: Int, limit: Int, query: String, sortBy: FileSortBy): List<Image> {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    val fields = SearchHelper.parse(query)
    val textField = fields.find { it.name == "text" }
    val queryText = textField?.value ?: ""
    return searchImagesCombined(
        queryText = queryText,
        extraQuery = query,
        limit = limit,
        offset = offset,
        sortBy = sortBy,
    ).map { it.toModel() }
}

fun SchemaBuilder.addImageSchema() {
    type<Image> {
        dataProperty("tags") {
            prepare { item -> item.id.value }
            loader { ids ->
                TagsLoader.load(ids, DataType.IMAGE)
            }
        }
    }
    type<ImageSearchStatus> {}
}
