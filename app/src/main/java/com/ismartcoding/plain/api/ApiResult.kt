package com.ismartcoding.plain.api

import com.ismartcoding.plain.i18n.*

import com.ismartcoding.lib.extensions.isOk
import com.ismartcoding.plain.features.locale.LocaleHelper
import io.ktor.client.statement.*

data class ApiResult(val response: HttpResponse?, val exception: Throwable? = null) {
    fun isOk(): Boolean {
        return response?.isOk() == true
    }

    fun errorMessage(): String {
        return exception?.toString() ?: response?.toString() ?: LocaleHelper.getStringSync(Res.string.unknown)
    }
}
