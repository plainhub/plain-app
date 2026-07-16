package com.ismartcoding.plain.web.http

/**
 * Standard WebSocket close codes used by the route handlers.
 *
 * Reference: RFC 6455 Section 7.4 (https://datatracker.ietf.org/doc/html/rfc6455#section-7.4)
 */
object WsCloseCode {
    /** 1008 — Policy violation (e.g. missing required query parameter). */
    const val POLICY_VIOLATION = 1008

    /** 1013 — Try again later (e.g. rate-limited or auth failed, retry later). */
    const val TRY_AGAIN_LATER = 1013
}
