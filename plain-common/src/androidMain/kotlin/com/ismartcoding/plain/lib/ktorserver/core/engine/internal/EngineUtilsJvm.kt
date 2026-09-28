/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.engine.internal

private val OS_NAME = System.getProperty("os.name", "")
    .lowercase()

internal fun escapeHostname(value: String): String {
    if (!OS_NAME.contains("windows")) return value
    if (value != "0.0.0.0") return value

    return "127.0.0.1"
}
