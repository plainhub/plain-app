package com.ismartcoding.plain.features.chat.peer

import com.ismartcoding.plain.features.notes.NotesApi
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.ManagedDeviceInfo
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.rust.graphql.changes
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import com.ismartcoding.plain.rust.graphql.SubscriptionUpdate
import com.ismartcoding.plain.rust.graphql.RustGraphqlSubscriptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import io.ktor.http.URLBuilder
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

internal class ManagedDeviceClient private constructor(
    private val peer: Peer,
    private val http: com.ismartcoding.plain.rust.graphql.PeerHttpClient,
    initialInfo: ManagedDeviceInfo,
    private val updates: (List<GraphqlOperation>) -> Flow<SubscriptionUpdate<GraphqlResponse>>,
) {
    companion object {
        private fun infoRequest() = GraphqlRequest<ManagedDeviceInfo>(
            "query { app { deviceName capabilities permissions appDir downloadsDir urlToken tlsCertificateSha256 } }", field = "app",
        )

        suspend fun load(
            peer: Peer,
            http: com.ismartcoding.plain.rust.graphql.PeerHttpClient = com.ismartcoding.plain.rust.graphql.PeerHttpClient(peer),
            updates: (List<GraphqlOperation>) -> Flow<SubscriptionUpdate<GraphqlResponse>> = { RustGraphqlSubscriptions.subscribePeerUpdates(peer, it) },
        ): ManagedDeviceClient = ManagedDeviceClient(peer, http, http.graphql.execute(infoRequest()), updates)
    }
    val api = http.graphql
    fun subscribe(request: com.ismartcoding.plain.rust.graphql.GraphqlOperation) = subscribe(listOf(request))
    fun subscribe(requests: List<GraphqlOperation>) = subscriptionUpdates(requests).transform { update ->
        when (update) {
            is SubscriptionUpdate.Data -> emit(update.value)
            SubscriptionUpdate.Attached -> Unit
            is SubscriptionUpdate.Failed -> throw update.error
        }
    }
    val deviceType: com.ismartcoding.plain.platform.system.DeviceType get() = peer.deviceType
    fun subscriptionUpdates(requests: List<GraphqlOperation>) = updates(requests)
    fun changes(requests: List<com.ismartcoding.plain.rust.graphql.GraphqlOperation>) =
        subscriptionUpdates(requests).changes()
    val peerId: String get() = peer.id
    val publicKey: String get() = peer.publicKey
    val key: String get() = peer.id + ":" + peer.publicKey
    private val _info = kotlinx.coroutines.flow.MutableStateFlow(initialInfo)
    val state = _info.asStateFlow()
    val currentInfo: ManagedDeviceInfo get() = _info.value
    val permissions = state.map { it.permissions }.distinctUntilChanged()

    suspend fun refreshPermissions(): List<com.ismartcoding.plain.platform.permissions.Permission> =
        api.execute(Query.appPermissions()).also { permissions -> _info.value = _info.value.copy(permissions = permissions) }

    private val uploads = com.ismartcoding.plain.features.files.upload.UploadChunksClient({ id, index, bytes ->
        http.uploadChunk(id, index, bytes, checkNotNull(currentInfo.tlsCertificateSha256) { "peer_https_unavailable" })
    }, api)

    suspend fun writeChunks(path: String, chunks: Iterator<ByteArray>): Boolean = uploads.replace(path, chunks)
    val notes = NotesApi(api)

    suspend fun info(): ManagedDeviceInfo = api.execute(infoRequest()).also { _info.value = it }

    suspend fun roots(): List<com.ismartcoding.plain.models.ManagedStorageRoot> = api.execute(GraphqlRequest(
        "query { mounts { name path } }", field = "mounts",
    ))

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    suspend fun importNoteImage(path: String, mimeType: String): String {
        val extension = com.ismartcoding.plain.platform.media.getExtensionFromMimeType(mimeType)
        val name = kotlin.uuid.Uuid.random().toString() + if (extension.isEmpty()) "" else ".$extension"
        return "app://note-images/" + uploads.create("app://note-images/$name") { sink ->
            checkNotNull(com.ismartcoding.plain.platform.files.streamContentUri(path, sink)) { "Unable to open selected image" }
        }
    }

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    suspend fun importAttachment(uri: String, fileName: String, mimeType: String): com.ismartcoding.plain.models.AppFile {
        val name = uploads.create("app://upload-${kotlin.uuid.Uuid.random()}") { sink ->
            checkNotNull(com.ismartcoding.plain.platform.files.streamContentUri(uri, sink)) { "Unable to read selected file" }
        }
        val uploaded = "app://$name"
        try {
            return api.execute(Mutation.importAppFile(uploaded, fileName, mimeType, true))
        } finally {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { api.execute(Mutation.deleteFiles(listOf(uploaded))) }
        }
    }

    suspend fun resource(path: String, thumbnailSize: Int = 0): com.ismartcoding.plain.models.ManagedResource {
        val info = currentInfo
        val current = http.current()
        val fileId = http.encryptResourceReference(info.urlToken, path)
        val base = current.address?.baseUrl ?: error("peer_address_unavailable")
        val url = managedResourceUrl(base, fileId, thumbnailSize)
        return com.ismartcoding.plain.models.ManagedResource(url, checkNotNull(info.tlsCertificateSha256) { "peer_https_unavailable" })
    }

    suspend fun downloadFile(path: String, name: String): String {
        val request = resource(path)
        val downloaded = com.ismartcoding.plain.features.files.RemoteFiles.download(request.url, name, request.tlsCertificateSha256)
        val latest = GraphqlApi.execute(Query.peers()).find { it.id == peer.id }
        check(latest?.isPaired() == true && latest.publicKey == peer.publicKey && latest.token == peer.token) { "peer_identity_changed" }
        return downloaded
    }
}

internal fun managedResourceUrl(base: String, fileId: String, thumbnailSize: Int): String =
    URLBuilder(base.trimEnd('/') + "/fs").apply {
        parameters.append("id", fileId)
        if (thumbnailSize > 0) {
            parameters.append("w", thumbnailSize.toString())
            parameters.append("h", thumbnailSize.toString())
            parameters.append("cc", "false")
        }
    }.buildString()
