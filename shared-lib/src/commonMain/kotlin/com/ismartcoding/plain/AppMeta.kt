package com.ismartcoding.plain

@PublishedApi
internal expect var buildTypeValue: String

@PublishedApi
internal expect var buildChannelValue: String

val buildType: String
    get() = buildTypeValue

val buildChannel: String
    get() = buildChannelValue