package com.ismartcoding.plain.api

import io.ktor.client.statement.*
import io.ktor.http.*

fun HttpResponse.isOk(): Boolean {
    return status == HttpStatusCode.OK
}