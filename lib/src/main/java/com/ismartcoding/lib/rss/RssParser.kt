package com.ismartcoding.lib.rss

import com.ismartcoding.lib.rss.internal.XmlFetcher
import com.ismartcoding.lib.rss.internal.XmlParser
import com.ismartcoding.lib.rss.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class RssParser internal constructor(
    private val xmlFetcher: XmlFetcher,
    private val xmlParser: XmlParser,
) {

    private val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Default

    internal interface Builder {
        /**
         * Creates a [RssParser] object
         */
        fun build(): RssParser
    }

    suspend fun getRssChannel(url: String): RssChannel = withContext(coroutineContext) {
        val parserInput = xmlFetcher.fetchXml(url)
        return@withContext xmlParser.parseXML(parserInput)
    }

    suspend fun parse(rawRssFeed: String): RssChannel = withContext(coroutineContext) {
        val parserInput = xmlFetcher.generateParserInputFromString(rawRssFeed)
        return@withContext xmlParser.parseXML(parserInput)
    }
}

