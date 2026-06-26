package com.ismartcoding.plain.api

import com.ismartcoding.plain.i18n.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString as getComposeString

data class ApiResult(val response: HttpResponse?, val exception: Throwable? = null) {
    fun isOk(): Boolean {
        return response?.isOk() == true
    }

    fun errorMessage(): String {
        return exception?.toString() ?: response?.toString() ?: runBlocking { getComposeString(Res.string.unknown) }
    }
}