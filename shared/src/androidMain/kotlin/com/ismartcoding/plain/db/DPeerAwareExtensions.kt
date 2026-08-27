package com.ismartcoding.plain.db

import com.ismartcoding.plain.chat.peer.transport.aware.AwareHttpClientFactory
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.lib.extensions.urlEncode

fun DPeer.getAwareFileUrl(fileId: String, port: Int): String =
    UrlHelper.buildUrl("https", AwareHttpClientFactory.AWARE_HOST, port, "/fs?id=${fileId.urlEncode()}")
