package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class PackageInstallPending(val id: ID, val updatedAt: Instant?, val isNew: Boolean)