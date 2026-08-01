package com.ismartcoding.plain.lib.rss

data class HttpException(
    val code: Int,
    override val message: String,
) : Exception()
