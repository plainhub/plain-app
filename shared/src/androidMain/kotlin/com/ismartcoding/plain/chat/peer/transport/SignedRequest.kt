package com.ismartcoding.plain.chat.peer.transport

data class SignedRequest(
    val body: String,
    val channelId: String,
)
