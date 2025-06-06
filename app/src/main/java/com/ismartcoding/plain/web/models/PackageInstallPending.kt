package com.ismartcoding.plain.web.models

import kotlinx.datetime.Instant

data class PackageInstallPending(val packageName: String, val updatedAt: Instant?, val isNew: Boolean)