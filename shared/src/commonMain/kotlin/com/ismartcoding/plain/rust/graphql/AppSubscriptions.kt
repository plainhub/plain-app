package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.models.ChatItemId
import com.ismartcoding.plain.models.MediaLibraryChange
import com.ismartcoding.plain.models.MediaScanProgress
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.rust.RustRuntime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

import com.ismartcoding.plain.features.chat.channel.ChannelRuntimeEvent
import com.ismartcoding.plain.features.images.search.ImageModelsSnapshot
import com.ismartcoding.plain.models.PomodoroAction

@OptIn(ExperimentalCoroutinesApi::class)
object AppSubscriptions {
    private val scope = RustRuntime.scope
    internal val peersChanged =
        subscription<Boolean>("peersChanged")
            .shareIn(scope, SharingStarted.WhileSubscribed())
    internal val channelsChanged =
        subscription<ChannelRuntimeEvent>(
            "channelsChanged",
            "{ changed invite { channelId ownerPeerId ownerPeerName } cancel { channelId " +
                "ownerPeerId } }",
        )
            .shareIn(scope, SharingStarted.WhileSubscribed())
    internal val mdnsChanged =
        subscription<Boolean>("mdnsChanged")
            .shareIn(scope, SharingStarted.WhileSubscribed())
    internal val nearbyDevicesChanged =
        subscription<Boolean>("nearbyDevicesChanged")
            .shareIn(scope, SharingStarted.WhileSubscribed())
    internal val downloadsChanged =
        subscription<Boolean>("downloadsChanged")
            .shareIn(scope, SharingStarted.WhileSubscribed())
    internal val imageModelsUpdated =
        subscription<ImageModelsSnapshot>(
            "imageModelsUpdated",
            "{ version status downloadProgress errorMessage modelSize modelDir " +
                "isIndexing totalImages indexedImages }",
        )
            .shareIn(scope, SharingStarted.WhileSubscribed())
    internal val pairingChanged =
        subscription<Boolean>("pairingChanged")
            .shareIn(scope, SharingStarted.WhileSubscribed())
    internal val pairingRequested =
        subscription<com.ismartcoding.plain.models.PairingRequestNotice>(
            "pairingRequested",
            "{ fromId fromName port deviceType ecdhPublicKey signaturePublicKey " +
                "timestamp ips signature fromIp awareSupported verificationCode }",
        )
            .shareIn(scope, SharingStarted.WhileSubscribed())
    internal val pomodoroAction =
        subscription<PomodoroAction>(
            "pomodoroAction",
            "{ today { date state timeLeftSec totalTimeSec completedCount currentRound " +
                "isRunning isPaused } completedState }",
        )
            .shareIn(scope, SharingStarted.WhileSubscribed())
    val contentChanged =
        subscription<Boolean>("contentChanged")
            .shareIn(scope, SharingStarted.WhileSubscribed())
    val permissionsUpdated =
        subscription<Boolean>("permissionsUpdated")
            .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)
    val messagesCreated =
        subscription<List<ChatItemId>>("messagesCreated", "{ id }")
            .shareIn(scope, SharingStarted.WhileSubscribed())
    val messagesUpdated =
        subscription<List<ChatItemId>>("messagesUpdated", "{ id }")
            .shareIn(scope, SharingStarted.WhileSubscribed())
    val messagesDeleted =
        subscription<String>("messagesDeleted")
            .shareIn(scope, SharingStarted.WhileSubscribed())
    private val storageGranted = permissionsUpdated.changes()
        .map { Permission.WRITE_EXTERNAL_STORAGE.isGranted() }
        .distinctUntilChanged()
    val mediaLibraryUpdated = storageGranted.flatMapLatest { granted ->
        if (granted) subscription<MediaLibraryChange>("mediaLibraryUpdated", "{ types }") else emptyFlow()
    }.shareIn(scope, SharingStarted.WhileSubscribed())
    val mediaScanProgress = storageGranted.flatMapLatest { granted ->
        if (granted) subscription<MediaScanProgress>("mediaScanProgress", "{ indexed pending total state error }") else emptyFlow()
    }.shareIn(scope, SharingStarted.WhileSubscribed())

    private inline fun <reified T> subscription(field: String, selection: String = ""): Flow<SubscriptionUpdate<T>> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                RustGraphqlSubscriptions.subscribe(GraphqlOperation("subscription { $field $selection }")).collect { response ->
                    if (response.errors.isNotEmpty()) throw GraphqlSubscriptionException(response.errors)
                    if (response.extensions?.get("subscriptionReady")?.jsonPrimitive?.booleanOrNull == true) {
                        emit(SubscriptionUpdate.Attached)
                    }
                    response.data?.get(field)?.let {
                        emit(SubscriptionUpdate.Data(JsonHelper.jsonDecodeFromElement<T>(it)))
                    }
                }
                return@flow
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: GraphqlSubscriptionException) {
                if (!error.requiresResynchronization) {
                    LogCat.e("GraphQL subscription $field", error)
                    emit(SubscriptionUpdate.Failed(error))
                    return@flow
                }
            } catch (error: Exception) {
                LogCat.e("GraphQL subscription $field", error)
                emit(SubscriptionUpdate.Failed(error))
                return@flow
            }
        }
    }
}
