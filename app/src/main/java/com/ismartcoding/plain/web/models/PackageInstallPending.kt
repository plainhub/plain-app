package com.ismartcoding.plain.web.models

import kotlin.time.Instant

data class PackageInstallPending(val packageName: String, val updatedAt: Instant?, val isNew: Boolean)