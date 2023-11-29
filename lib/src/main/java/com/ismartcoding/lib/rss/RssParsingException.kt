package com.ismartcoding.lib.rss

data class RssParsingException(
    override val message: String,
    override val cause: Throwable,
) : Exception()
