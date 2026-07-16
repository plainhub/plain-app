package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.crypto.sha256
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.lib.extensions.htmlToPlainText
import com.ismartcoding.plain.lib.extensions.toHexString
import com.ismartcoding.plain.lib.html2md.MDConverter
import com.ismartcoding.plain.lib.rss.DateParser
import com.ismartcoding.plain.lib.rss.model.RssItem
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Convert an [RssItem] into a [DFeedEntry] for storage.
 *
 * Pure-Kotlin port of the previous androidMain implementation: replaces
 * `java.util.UUID` with `kotlin.uuid.Uuid` and `CryptoHelper.sha1` with
 * the commonMain `sha256` (the resulting `rawId` is a derived hash used
 * only for indexing, never compared against legacy SHA-1 values, so the
 * digest algorithm change is safe).
 */
@OptIn(ExperimentalUuidApi::class)
fun RssItem.toDFeedEntry(
    feedId: String,
    feedUrl: String,
): DFeedEntry {
    val item = DFeedEntry()
    item.rawId =
        sha256(
            (feedId + "_" + (link ?: title ?: Uuid.random().toString())).encodeToByteArray(),
        ).toHexString()

    item.feedId = feedId
    item.title = if (title != null) title.htmlToPlainText() else ""

    val feedBaseUrl = HtmlUtils.getBaseUrl(feedUrl)
    item.description = HtmlUtils.improveHtmlContent(content ?: description ?: "", feedBaseUrl)
    item.description = MDConverter().convert(item.description)
    item.url = link ?: ""

    item.image = image ?: ""

    item.author = author?.ifEmpty { sourceName ?: "" } ?: ""

    if (pubDate != null) {
        val dateMillis = DateParser.parseDate(pubDate!!)
        if (dateMillis != null && dateMillis < item.publishedAt.toEpochMilliseconds()) {
            item.publishedAt = Instant.fromEpochMilliseconds(dateMillis)
        }
    }

    return item
}
