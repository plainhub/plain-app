package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class PackageInstallPending(val packageName: String, val updatedAt: Instant?, val isNew: Boolean)