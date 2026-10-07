package com.ismartcoding.plain.httpserver.models

import kotlin.time.Instant

data class PackageInstallPending(val id: ID, val updatedAt: Instant?, val isNew: Boolean)