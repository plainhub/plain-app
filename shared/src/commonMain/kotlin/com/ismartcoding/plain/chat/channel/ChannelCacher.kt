package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.lib.extensions.toSortName
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DChat
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.helpers.Base64Lenient
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object ChannelCacher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val channelsMap = MutableStateFlow<Map<String, ChannelRuntime>>(emptyMap())

    val channels: StateFlow<List<DChatChannel>> = combine(channelsMap, ChatCacher.latestChatMap) { c, chatCache ->
        c.values.map { it.channel }.sortedWith(
            compareByDescending<DChatChannel> { chatCache[it.id]?.createdAt ?: Instant.DISTANT_PAST }
                .thenBy { it.name.toSortName() },
        )
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Read-only channel view; use [ChannelManager] for mutations. */
    fun getChannel(channelId: String): DChatChannel? = channelsMap.value[channelId]?.channel

    fun getKeyBytes(channelId: String): ByteArray? = channelsMap.value[channelId]?.keyBytes?.takeIf { it.isNotEmpty() }

    suspend fun load() = withIO {
        val channels = RustChannelStore.getAll()
        val runtimeMap = channels.associate { channel ->
            val keyBytes = if (channel.key.isNotEmpty()) Base64Lenient.decode(channel.key) else ByteArray(0)
            channel.id to ChannelRuntime(channel, keyBytes)
        }
        channelsMap.value = runtimeMap
    }
}
