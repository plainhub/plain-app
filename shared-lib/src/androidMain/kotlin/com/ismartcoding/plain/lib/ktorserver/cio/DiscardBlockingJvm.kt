/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
/**
 * Vendored from io.ktor:ktor-http-cio:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.cio

import io.ktor.utils.io.*
import kotlinx.coroutines.*

internal fun ByteReadChannel.discardBlocking() {
    runBlocking {
        discard()
    }
}
