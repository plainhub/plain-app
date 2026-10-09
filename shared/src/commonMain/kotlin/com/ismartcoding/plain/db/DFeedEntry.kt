package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.generateId
import kotlin.time.Instant
import kotlinx.serialization.Serializable

// https://validator.w3.org/feed/docs/rss2.html
// https://validator.w3.org/feed/docs/atom.html
@Serializable
data class DFeedEntry(
    override var id: String = generateId(),
    var title: String = "",
    var url: String = "",
    var image: String = "",
    var description: String = "",
    var author: String = "",
    var content: String = "",

    var feedId: String = "",

    var rawId: String = "",

    var publishedAt: Instant = TimeHelper.now(),

    var read: Boolean = false,

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
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
