package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.crypto.sha256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val DOMAIN = "plain-chat-pairing-v1"

@OptIn(ExperimentalEncodingApi::class)
fun deriveLoginChatKey(token: String): String =
    Base64.encode(sha256(DOMAIN.encodeToByteArray() + Base64.decode(token)))
