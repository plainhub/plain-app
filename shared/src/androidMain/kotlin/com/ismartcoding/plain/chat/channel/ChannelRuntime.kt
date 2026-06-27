package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.db.DChatChannel

data class ChannelRuntime(
    val channel: DChatChannel,
    val keyBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChannelRuntime

        if (channel != other.channel) return false
        if (!keyBytes.contentEquals(other.keyBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = channel.hashCode()
        result = 31 * result + keyBytes.contentHashCode()
        return result
    }
}