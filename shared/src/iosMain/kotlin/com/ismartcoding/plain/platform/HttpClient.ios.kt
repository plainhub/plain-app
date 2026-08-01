package com.ismartcoding.plain.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpEngine(spec: HttpClientSpec): HttpClientEngine =
    when (spec) {
        is HttpClientSpec.Crypto -> Darwin.create()
        else -> Darwin.create()
    }
