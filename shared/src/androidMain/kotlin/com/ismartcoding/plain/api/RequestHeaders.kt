package com.ismartcoding.plain.api

import android.util.Base64
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.getAppVersion
import com.ismartcoding.plain.helpers.PhoneHelper
import okhttp3.Request

fun Request.Builder.addClientHeaders(): Request.Builder = apply {
    addHeader("c-id", TempData.clientId)
    addHeader("c-platform", "android")
    addHeader(
        "c-name",
        Base64.encodeToString(PhoneHelper.getDeviceName(appContext).toByteArray(), Base64.NO_WRAP),
    )
    addHeader("c-version", getAppVersion())
}