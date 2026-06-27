package com.ismartcoding.plain.chat.channel

import android.util.Base64
import com.ismartcoding.plain.lib.extensions.toSortName
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChatChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

object ChannelCacher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val channelsMap = MutableStateFlow<Map<String, ChannelRuntime>>(emptyMap())

    val channels: StateFlow<List<DChatChannel>> = channelsMap
        .map { it.values.map { runtime -> runtime.channel }.sortedBy { c -> c.name.toSortName() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun getChannel(channelId: String): DChatChannel? = channelsMap.value[channelId]?.channel

    fun getKeyBytes(channelId: String): ByteArray? = channelsMap.value[channelId]?.keyBytes?.takeIf { it.isNotEmpty() }

    fun updateChannel(channel: DChatChannel) {
        val current = channelsMap.value
        val runtime = current[channel.id] ?: return
        if (runtime.channel === channel) return
        channelsMap.value = current + (channel.id to runtime.copy(channel = channel))
    }

    fun removeChannel(channelId: String) {
        val current = channelsMap.value
        if (!current.containsKey(channelId)) return
        channelsMap.value = current - channelId
    }

    suspend fun load() = withIO {
        val channels = AppDatabase.instance.chatChannelDao().getAll()
        val runtimeMap = channels.associate { channel ->
            val keyBytes = if (channel.key.isNotEmpty()) Base64.decode(channel.key, Base64.NO_WRAP) else ByteArray(0)
            channel.id to ChannelRuntime(channel, keyBytes)
        }
        channelsMap.value = runtimeMap
    }
}
