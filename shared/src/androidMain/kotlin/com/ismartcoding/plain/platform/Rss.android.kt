package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.rss.RssParser
import com.ismartcoding.plain.lib.rss.model.RssChannel
import com.ismartcoding.plain.helpers.withIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

actual suspend fun fetchRssChannel(url: String): RssChannel = withIO {
    val r = KtorClientFactory.httpClient().get(url)
    if (r.status != HttpStatusCode.OK) {
        throw Exception("HTTP ${r.status.value} ${r.status.description}")
    }
    val xmlString = r.bodyAsText()
    val rssParser = RssParser()
    rssParser.parse(xmlString)
}
