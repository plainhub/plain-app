package com.ismartcoding.plain.features.chat.peer

import com.ismartcoding.plain.IData
import com.ismartcoding.plain.features.feeds.FeedsApi
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.files.FileWriteAction
import com.ismartcoding.plain.features.notes.NotesApi
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.ManagedDeviceInfo
import com.ismartcoding.plain.models.ManagedStorageRoot
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.rust.native.nativeCall
import com.ismartcoding.plain.helpers.SearchHelper
import io.ktor.http.URLBuilder

internal class ManagedDeviceClient(private val peer: Peer) {
    val api = GraphqlApi.forPeer(peer)
    val notes = NotesApi(api)
    val feeds = FeedsApi(api)

    suspend fun info(): ManagedDeviceInfo = api.execute(GraphqlRequest(
        "query { app { deviceName capabilities appDir downloadsDir urlToken } }", field = "app",
    ))

    suspend fun roots(): List<ManagedStorageRoot> = api.execute(GraphqlRequest(
        "query { mounts { name path } }", field = "mounts",
    ))

    suspend fun rows(feature: AppFeatureType, path: String, query: String, offset: Int, limit: Int, feedId: String = ""): List<IData> = when (feature) {
        AppFeatureType.FILES -> api.execute(Query.files(path, query, FileSortBy.NAME_ASC, offset, limit))
        AppFeatureType.NOTES -> notes.search(query, limit, offset)
        AppFeatureType.FEEDS -> if (feedId.isEmpty()) {
            feeds.getAll().filter { query.isEmpty() || it.name.contains(query, true) || it.url.contains(query, true) }
        } else {
            val escaped = feedId.replace("\\", "\\\\").replace("\"", "\\\"")
            api.execute(Query.feedEntries(SearchHelper.buildTextFilter(query) + " feed_id:\"$escaped\"", limit, offset))
        }
        AppFeatureType.APPS -> api.execute(Query.packages(query, offset, limit, FileSortBy.NAME_ASC))
        else -> api.execute(Query.mediaRows(mediaType(feature), query, limit, offset, FileSortBy.DATE_DESC))
    }

    suspend fun createDirectory(parent: String, name: String) {
        api.execute(Mutation.fileWrite(FileWriteAction.CREATE_DIRECTORY, parent.trimEnd('/') + "/" + name, "", false))
    }

    suspend fun cacheFile(path: String, name: String): String {
        val info = info()
        val current = GraphqlApi.execute(Query.peers()).find { it.id == peer.id } ?: error("peer_removed")
        check(current.isPaired() && current.publicKey == peer.publicKey && current.token == peer.token) { "peer_identity_changed" }
        val fileId: String = nativeCall { RustNativeBindings.encryptResourceReference(info.urlToken, path) }
        val base = current.address?.baseUrl ?: error("peer_address_unavailable")
        val url = URLBuilder(base.trimEnd('/') + "/fs").apply { parameters.append("id", fileId) }.buildString()
        val cached = GraphqlApi.execute(Mutation.cacheRemoteFile(url, name))
        val latest = GraphqlApi.execute(Query.peers()).find { it.id == peer.id }
        check(latest?.isPaired() == true && latest.publicKey == peer.publicKey && latest.token == peer.token) { "peer_identity_changed" }
        return cached
    }
}

internal fun mediaType(feature: AppFeatureType): DataType = when (feature) {
    AppFeatureType.IMAGES -> DataType.IMAGE
    AppFeatureType.VIDEOS -> DataType.VIDEO
    AppFeatureType.AUDIO -> DataType.AUDIO
    AppFeatureType.DOCS -> DataType.DOC
    else -> error("Unsupported managed media feature: $feature")
}
