package com.ismartcoding.lib.rss

import com.ismartcoding.lib.rss.internal.XmlFetcher
import com.ismartcoding.lib.rss.internal.XmlParser
import kotlinx.coroutines.Dispatchers
import okhttp3.Call
import okhttp3.OkHttpClient
import java.nio.charset.Charset


class RssParserBuilder(
    private val callFactory: Call.Factory? = null,
    private val charset: Charset? = null,
) : RssParser.Builder {
    override fun build(): RssParser {
        val client = callFactory ?: OkHttpClient()
        return RssParser(
            xmlFetcher = XmlFetcher(
                callFactory = client,
                charset = charset,
            ),
            xmlParser = XmlParser(
                charset = charset,
                dispatcher = Dispatchers.IO,
            ),
        )
    }
}

fun RssParser(): RssParser = RssParserBuilder().build()
