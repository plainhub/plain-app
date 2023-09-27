package com.ismartcoding.lib.channel

class ChannelEvent<T>(val event: T) {
    override fun toString(): String {
        return "event = $event"
    }
}
