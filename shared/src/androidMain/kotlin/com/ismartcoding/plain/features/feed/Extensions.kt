package com.ismartcoding.plain.features.feed

import androidx.core.text.HtmlCompat
import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.lib.html2md.MDConverter
import com.ismartcoding.plain.lib.rss.DateParser
import com.ismartcoding.plain.lib.rss.model.RssItem
import com.ismartcoding.plain.db.DFeedEntry
import kotlin.time.Instant
import java.util.UUID


fun RssItem.toDFeedEntry(
    feedId: String,
    feedUrl: String,
): DFeedEntry {
    val item = DFeedEntry()
    item.rawId =
        CryptoHelper.sha1(
            (feedId + "_" + (link ?: title ?: UUID.randomUUID().toString())).toByteArray(),
        )
    item.feedId = feedId
    if (title != null) {
        item.title = HtmlCompat.fromHtml(title!!, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().replace("\n", " ").trim()
    } else {
        item.title = ""
    }

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
