package com.ismartcoding.plain.api

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.getAppVersion
import com.ismartcoding.plain.helpers.PhoneHelper
import okhttp3.Request
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun Request.Builder.addClientHeaders(): Request.Builder = apply {
    addHeader("c-id", TempData.clientId)
    addHeader("c-platform", "android")
    addHeader(
        "c-name",
        Base64.encode(PhoneHelper.getDeviceName(appContext).toByteArray()),
    )
    addHeader("c-version", getAppVersion())
}
