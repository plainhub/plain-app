package com.ismartcoding.plain.db

import com.ismartcoding.plain.enums.SessionType
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

data class DSession(
    var clientId: String = "",

    var name: String = "",

    var type: SessionType = SessionType.WEB,

    var clientIp: String = "",

    var osName: String = "",

    var osVersion: String = "",

    var browserName: String = "",

    var browserVersion: String = "",

    var token: String = "",

    var lastActiveAt: Instant? = null,

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
)
