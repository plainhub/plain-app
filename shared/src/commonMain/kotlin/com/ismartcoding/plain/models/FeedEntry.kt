package com.ismartcoding.plain.models

import com.ismartcoding.plain.IData
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Required

@Serializable
data class FeedEntry(
    @Required var feedId: String = "",
    @Required override var id: String = "",
    @Required var title: String = "",
    @Required var url: String = "",
    @Required var image: String = "",
    @Required var description: String = "",
    @Required var author: String = "",
    @Required var content: String = "",
    @Required var rawId: String = "",
    @Required var read: Boolean = false,
    @Required var publishedAt: Instant = TimeHelper.now(),
    @Required var createdAt: Instant = TimeHelper.now(),
    @Required var updatedAt: Instant = TimeHelper.now(),
    var feed: Feed? = null,
) : IData {
    fun getSummary(): String {
        val regex = Regex("!\\[.*?\\]\\(.*?\\)|!\\[.*?\\]\\[.*?\\]|<img.*?>", RegexOption.IGNORE_CASE)
        return description.replace(regex, "🖼").replace("\n", "").replaceFirst("^\\s*".toRegex(), "")
    }

    // Heuristic: feeds that ship full articles (content:encoded merged into
    // description) produce long bodies; summary feeds stay short. No web fetch needed.
    val isFullContent: Boolean
        get() = description.length >= FULL_CONTENT_MIN_LENGTH
}

private const val FULL_CONTENT_MIN_LENGTH = 500
