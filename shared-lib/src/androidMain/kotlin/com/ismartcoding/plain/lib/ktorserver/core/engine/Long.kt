/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.engine

private val longStrings = Array(1024) {
    it.toString()
}

internal fun Long.toStringFast() = if (this in 0..1023) longStrings[toInt()] else toString()
