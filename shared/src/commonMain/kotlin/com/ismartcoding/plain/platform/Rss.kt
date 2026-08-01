package com.ismartcoding.plain.platform

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.rss.RssParser
import com.ismartcoding.plain.lib.rss.model.RssChannel
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

suspend fun fetchRssChannel(url: String): RssChannel = withIO {
    val r = KtorClientFactory.httpClient().get(url)
    if (r.status != HttpStatusCode.OK) {
        throw Exception("HTTP ${r.status.value} ${r.status.description}")
    }
    val xmlString = r.bodyAsText()
    val rssParser = RssParser()
    rssParser.parse(xmlString)
}
