package com.ismartcoding.plain.enums

enum class NearbyMessageType {
    DISCOVER,
    DISCOVER_REPLY,
    PAIR_REQUEST,
    PAIR_RESPONSE,
    PAIR_CANCEL;

    fun toPrefix(): String {
        return "${this}:"
    }
}