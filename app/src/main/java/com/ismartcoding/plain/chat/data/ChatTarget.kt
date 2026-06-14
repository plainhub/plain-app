package com.ismartcoding.plain.chat.data

data class ChatTarget(val toId: String, val type: ChatTargetType) {
    companion object {
        fun parseId(id: String): ChatTarget {
            return when {
                id.startsWith("channel:") -> ChatTarget(id.removePrefix("channel:"), ChatTargetType.CHANNEL)
                id.startsWith("peer:") -> ChatTarget(id.removePrefix("peer:"), ChatTargetType.PEER)
                else -> ChatTarget(id, ChatTargetType.LOCAL)
            }
        }
    }
}
