/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
/**
 * Vendored from io.ktor:ktor-websockets:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.websocket

import io.ktor.util.internal.*
import kotlinx.coroutines.*

/**
 * Raised when peers send frames which violate the Websocket RFC
 *
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class ProtocolViolationException(
    public val violation: String
) : Exception(), CopyableThrowable<ProtocolViolationException> {
    override val message: String
        get() = "Received illegal frame: $violation"

    override fun createCopy(): ProtocolViolationException = ProtocolViolationException(violation).also {
        it.initCauseBridge(this)
    }
}
