package com.ismartcoding.plain.lib.rss

import com.ismartcoding.plain.lib.opml.SimpleXmlReader
import com.ismartcoding.plain.lib.rss.internal.AtomKeyword
import com.ismartcoding.plain.lib.rss.internal.RssKeyword
import com.ismartcoding.plain.lib.rss.internal.contains
import com.ismartcoding.plain.lib.rss.internal.extractAtomContent
import com.ismartcoding.plain.lib.rss.internal.extractRSSContent
import com.ismartcoding.plain.lib.rss.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class RssParser {

    private val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Default

    suspend fun parse(rawRssFeed: String): RssChannel = withContext(coroutineContext) {
        val reader = SimpleXmlReader(rawRssFeed)

        var rssChannel: RssChannel? = null

        var eventType = reader.next()
        while (eventType != SimpleXmlReader.END_DOCUMENT) {
            if (eventType == SimpleXmlReader.START_TAG) {
                if (reader.contains(RssKeyword.Rss)) {
                    rssChannel = extractRSSContent(reader)
                } else if (reader.contains(AtomKeyword.Feed.Atom)) {
                    rssChannel = extractAtomContent(reader)
                }
            }
            eventType = reader.next()
        }

        rssChannel
            ?: throw IllegalArgumentException(
                "The provided XML is not supported. Only RSS and Atom feeds are supported",
            )
    }
}
