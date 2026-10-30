package com.ismartcoding.plain.features.media

import com.ismartcoding.plain.IData
import com.ismartcoding.plain.features.contacts.ContactsClient
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.telephony.CallsClient
import com.ismartcoding.plain.features.telephony.sms.RustSmsQuery
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.rust.graphql.escapeQueryValue
import com.ismartcoding.plain.models.MediaBucket
import com.ismartcoding.plain.platform.media.getMediaItemUriString
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.selectionQuery

object MediaCatalogClient : MediaCatalogApi()

open class MediaCatalogApi internal constructor(
    protected val api: GraphqlClient = GraphqlApi,
    private val shareUri: suspend (DataType, String, String) -> String = { type, id, path -> getMediaItemUriString(type, id, path) },
) {
    suspend fun rows(type: DataType, query: String, limit: Int, offset: Int, sortBy: FileSortBy): List<IData> {
        when (type) {
            DataType.CALL -> return CallsClient.search(query, limit, offset)
            DataType.CONTACT -> return ContactsClient.search(query, limit, offset)
            DataType.SMS -> return RustSmsQuery.search(query, limit, offset)
            else -> Unit
        }
        return api.execute(Query.mediaRows(type, query, limit, offset, sortBy))
    }

    suspend fun artistTracks(artist: String, sortBy: FileSortBy): List<Audio> {
        val query = "artist:=${escapeQueryValue(artist)}"
        val items = mutableListOf<Audio>()
        val pageSize = 500
        do {
            val page = rows(DataType.AUDIO, query, pageSize, items.size, sortBy)
            items.addAll(page.map { it as Audio })
        } while (page.size == pageSize)
        return items
    }

    suspend fun shareUris(type: DataType, ids: Collection<String>): List<String> =
        readSelectedMediaPaths(Query.mediaRoot(type), ids, api)
            .map { (id, path) -> shareUri(type, id, path) }

    suspend fun count(type: DataType, query: String): Int {
        when (type) {
            DataType.CALL -> return CallsClient.count(query)
            DataType.CONTACT -> return ContactsClient.count(query)
            DataType.SMS -> return RustSmsQuery.count(query)
            else -> Unit
        }
        return api.execute(Query.mediaCount(type, query))
    }

    suspend fun docExtGroups(query: String): List<Pair<String, Int>> =
        api.execute(Query.docExtGroups(query)).map { it.ext to it.count }

    suspend fun buckets(dataType: DataType): List<MediaBucket> =
        api.execute(Query.mediaBuckets(dataType))
}

internal suspend fun readSelectedMediaPaths(
    root: String,
    ids: Collection<String>,
    api: GraphqlClient = GraphqlApi,
): List<Pair<String, String>> {
    if (ids.isEmpty()) return emptyList()
    val selected = ids.distinct()
    val rows = api.execute(
        Query.selectedMediaPaths(root, selectionQuery(selected), selected.size),
    )
    val paths = rows.associate { it.id to it.path }
    return selected.map { id -> id to paths.getValue(id) }
}
