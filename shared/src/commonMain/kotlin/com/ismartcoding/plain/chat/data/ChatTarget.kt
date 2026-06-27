package com.ismartcoding.plain.chat.data

data class ChatTarget(val toId: String, val type: ChatTargetType) {
    val encodedToId: String
        get() = when (type) {
            ChatTargetType.CHANNEL -> "channel:$toId"
            ChatTargetType.PEER -> "peer:$toId"
        }

    fun isLocal(): Boolean {
        return toId == "local"
    }

    companion object {
        fun parseId(id: String): ChatTarget {
            return if (id.startsWith("channel:")) {
                ChatTarget(id.removePrefix("channel:"), ChatTargetType.CHANNEL)
            } else {
                ChatTarget(id.removePrefix("peer:"), ChatTargetType.PEER)
            }
        }
    }
}