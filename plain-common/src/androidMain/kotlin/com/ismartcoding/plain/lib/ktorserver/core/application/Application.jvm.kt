package com.ismartcoding.plain.lib.ktorserver.core.application

import com.ismartcoding.plain.lib.ktorserver.core.engine.WORKING_DIRECTORY_PATH

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

internal fun defaultWatchPaths(): List<String> = listOf(WORKING_DIRECTORY_PATH)
