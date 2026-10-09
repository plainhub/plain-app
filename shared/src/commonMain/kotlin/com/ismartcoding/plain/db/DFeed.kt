package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import com.ismartcoding.plain.lib.generateId
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class DFeed(
    override var id: String = generateId(),
    var name: String = "",
    var url: String = "",

    var logo: String = "",

    var fetchContent: Boolean = false,

    var entryCount: Int = 0,

    // Last sync attempt state, written by FeedFetcher after every sync;
    // the UI (banner, drawer dot, sheet status card) derives from it.
    var lastSyncAt: Instant? = null,

    // Error payload of the last attempt.
    // Empty code = success.
    var lastError: DFeedError = DFeedError(),

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
) : IData {
    /** True when the last sync attempt failed. */
    val hasSyncError: Boolean
        get() = lastError.code.isNotEmpty()
}

@Serializable
data class DFeedError(
    val code: String = "",
    val detail: String = "",
)

private val feedErrorJson = Json { ignoreUnknownKeys = true }

fun DFeedError.toJSONString(): String = feedErrorJson.encodeToString(DFeedError.serializer(), this)

fun parseFeedError(json: String): DFeedError =
    try {
        feedErrorJson.decodeFromString(DFeedError.serializer(), json)
    } catch (_: Exception) {
        DFeedError()
    }

data class DFeedCount(var id: String, var entryCount: Int)
