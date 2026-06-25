package com.ismartcoding.plain.lib.rss

data class RssParsingException(
    override val message: String,
    override val cause: Throwable,
) : Exception()
