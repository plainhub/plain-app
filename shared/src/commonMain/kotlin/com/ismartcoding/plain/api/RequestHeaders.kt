package com.ismartcoding.plain.api

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.platform.getAppVersion
import com.ismartcoding.plain.platform.getDeviceName
import com.ismartcoding.plain.platform.getPlatformName
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.headers
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun HttpRequestBuilder.addClientHeaders() {
    headers {
        append("c-id", TempData.clientId)
        append("c-platform", getPlatformName())
        append("c-name", Base64.encode(getDeviceName().encodeToByteArray()))
        append("c-version", getAppVersion())
    }
}
