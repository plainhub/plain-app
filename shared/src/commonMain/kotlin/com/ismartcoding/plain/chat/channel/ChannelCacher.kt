package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.lib.extensions.toSortName
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DChatChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.ismartcoding.plain.helpers.Base64Lenient
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object ChannelCacher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val channelsMap = MutableStateFlow<Map<String, ChannelRuntime>>(emptyMap())

    val channels: StateFlow<List<DChatChannel>> = channelsMap
        .map { it.values.map { runtime -> runtime.channel }.sortedBy { c -> c.name.toSortName() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** 返回指定 channel 的只读视图。调用方不应直接修改返回对象；
     *  修改请使用 [mutateChannel]。 */
    fun getChannel(channelId: String): DChatChannel? = channelsMap.value[channelId]?.channel

    fun getKeyBytes(channelId: String): ByteArray? = channelsMap.value[channelId]?.keyBytes?.takeIf { it.isNotEmpty() }

    /**
     * 在 [channelId] 对应 channel 的副本上执行 [block] 进行修改，
     * 然后将修改后的副本写回缓存和数据库。
     *
     * 使用副本确保缓存内的旧对象不被破坏，从而让 [channels] StateFlow
     * 能通过引用差异检测到变化并发射更新（StateFlow 的 distinctUntilChanged
     * 基于内容比较，原地修改缓存引用会导致新旧 list 内容相同而不发射）。
     *
     * 内部已通过 [withIO] 切换到 IO dispatcher，调用方无需自行指定。
     *
     * @return 修改后的新 channel 实例；若 channelId 不存在则返回 null。
     */
    suspend fun mutateChannel(channelId: String, block: (DChatChannel) -> Unit): DChatChannel? = withIO {
        val current = channelsMap.value
        val runtime = current[channelId] ?: return@withIO null
        val newChannel = runtime.channel.copy()
        block(newChannel)
        AppDatabase.instance.chatChannelDao().update(newChannel)
        channelsMap.value = current + (channelId to runtime.copy(channel = newChannel))
        newChannel
    }

    fun removeChannel(channelId: String) {
        val current = channelsMap.value
        if (!current.containsKey(channelId)) return
        channelsMap.value = current - channelId
    }

    suspend fun load() = withIO {
        val channels = AppDatabase.instance.chatChannelDao().getAll()
        val runtimeMap = channels.associate { channel ->
            val keyBytes = if (channel.key.isNotEmpty()) Base64Lenient.decode(channel.key) else ByteArray(0)
            channel.id to ChannelRuntime(channel, keyBytes)
        }
        channelsMap.value = runtimeMap
    }
}
