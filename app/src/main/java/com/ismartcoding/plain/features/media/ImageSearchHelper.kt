package com.ismartcoding.plain.features.media

import android.content.Context
import com.ismartcoding.plain.ai.ImageSearchManager
import com.ismartcoding.plain.features.file.FileSortBy

object ImageSearchHelper {
    /**
     * Returns a combined list of images matching the query by filename (or other fields) and by semantic search.
     * The returned list is deduplicated and ordered: filename matches first, then semantic-only matches.
     * If the query is blank or the model is not ready, only filename search is used.
     * @param queryText the user query (natural language or filename)
     * @param extraQuery the full query string for filename search (e.g. with tags, trash, etc)
     * @param context Android context
     * @param limit max number of results
     * @param offset offset for pagination
     * @param sortBy sort order
     */
    suspend fun searchCombinedAsync(
        context: Context,
        queryText: String,
        extraQuery: String,
        limit: Int,
        offset: Int,
        sortBy: FileSortBy
    ): List<com.ismartcoding.plain.data.DImage> {
        if (queryText.isNotBlank() && ImageSearchManager.isModelReady()) {
            val semanticResults = ImageSearchManager.search(queryText)
            val filenameItems = ImageMediaStoreHelper.searchAsync(context, extraQuery, Int.MAX_VALUE, 0, sortBy)
            val filenameIds = filenameItems.map { it.id }.toSet()
            val combined = filenameItems.toMutableList()
            val semanticOnlyIds = semanticResults.map { it.imageId }.filter { it !in filenameIds }
            if (semanticOnlyIds.isNotEmpty()) {
                val idsQuery = "ids:${semanticOnlyIds.joinToString(",")} trash:false"
                val semanticItems = ImageMediaStoreHelper.searchAsync(context, idsQuery, semanticOnlyIds.size, 0, sortBy)
                val idOrder = semanticOnlyIds.withIndex().associate { it.value to it.index }
                combined.addAll(semanticItems.sortedBy { idOrder[it.id] ?: Int.MAX_VALUE })
            }
            return combined.drop(offset).take(limit)
        } else {
            return ImageMediaStoreHelper.searchAsync(context, extraQuery, limit, offset, sortBy)
        }
    }

    suspend fun countCombinedAsync(
        context: Context,
        queryText: String,
        extraQuery: String
    ): Int {
        if (queryText.isNotBlank() && ImageSearchManager.isModelReady()) {
            val semanticResults = ImageSearchManager.search(queryText)
            val filenameIds = ImageMediaStoreHelper.getIdsAsync(context, extraQuery)
            val semanticOnlyIds = semanticResults.map { it.imageId }.filter { it !in filenameIds }.toSet()
            return filenameIds.size + semanticOnlyIds.size
        } else {
            return ImageMediaStoreHelper.countAsync(context, extraQuery)
        }
    }
}
