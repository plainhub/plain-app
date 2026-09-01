/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-forwarded-header:3.5.2.
 */


package com.ismartcoding.plain.lib.ktorserver

internal fun String.isNotHostAddress(): Boolean {
    return if (contains(':')) {
        return true
    } else {
        none { it.isLetter() }
    }
}
