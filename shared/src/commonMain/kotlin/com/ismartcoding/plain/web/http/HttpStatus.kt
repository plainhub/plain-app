package com.ismartcoding.plain.web.http

/**
 * Standard HTTP status codes used by the route handlers.
 *
 * Only the subset actually referenced by the commonMain routes is listed —
 * keep this file lean and add new constants as the codebase needs them.
 */
object HttpStatus {
    const val OK = 200
    const val CREATED = 201
    const val NO_CONTENT = 204
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val GONE = 410
    const val TOO_MANY_REQUESTS = 429
    const val INTERNAL_SERVER_ERROR = 500
}
