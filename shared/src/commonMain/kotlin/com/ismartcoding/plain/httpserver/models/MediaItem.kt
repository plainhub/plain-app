package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInterface
import kotlin.time.Instant

/**
 * Fields shared by every media-library item type (Audio/Image/Video/Doc).
 * Clients define one fragment on this interface and spread it into any media
 * list query; future cross-type media queries can return [MediaItem].
 */
@GraphQLInterface
interface MediaItem {
    val id: ID
    val title: String
    val path: String
    val size: Long
    val bucketId: ID
    val createdAt: Instant
    val updatedAt: Instant
}
