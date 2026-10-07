package com.ismartcoding.plain.httpserver.models

/** Result of a synchronous bulk mutation: how many items were affected. */
data class ActionResult(
    val affectedCount: Int,
)
