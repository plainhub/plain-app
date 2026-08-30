package com.ismartcoding.plain.api

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.platform.getAppVersion
import com.ismartcoding.plain.platform.getDeviceName
import com.ismartcoding.plain.platform.getPlatformName
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Returns the common client identification headers (c-id, c-platform, c-version).
 *
 * Used by plain HTTP requests sent through [com.ismartcoding.plain.platform.PlainHttpClient]
 * and by non-HTTP transports (e.g. BLE) that need to forward the same metadata
 * to the peer's HTTP router.
 */
@OptIn(ExperimentalEncodingApi::class)
fun clientHeadersMap(): Map<String, String> = mapOf(
    "c-id" to TempData.clientId,
    "c-platform" to getPlatformName(),
    "c-version" to getAppVersion(),
)

/**
 * Client identification headers merged with [extra] entries, ready to be passed
 * as the header map of a [com.ismartcoding.plain.platform.PlainRequest].
 */
@OptIn(ExperimentalEncodingApi::class)
fun clientHeadersWith(vararg extra: Pair<String, String>): Map<String, String> =
    clientHeadersMap() + extra
