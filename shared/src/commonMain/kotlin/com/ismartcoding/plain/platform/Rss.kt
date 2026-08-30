package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.rss.RssParser
import com.ismartcoding.plain.lib.rss.model.RssChannel

suspend fun fetchRssChannel(url: String): RssChannel = withIO {
    val r = createHttpClient().get(url)
    r.use {
        if (!it.isOk()) {
            throw Exception("HTTP ${it.status}")
        }
        val xmlString = it.bodyAsText()
        val rssParser = RssParser()
        rssParser.parse(xmlString)
    }
}
