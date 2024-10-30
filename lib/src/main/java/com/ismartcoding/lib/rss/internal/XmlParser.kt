package com.ismartcoding.lib.rss.internal

import com.ismartcoding.lib.rss.RssParsingException
import com.ismartcoding.lib.rss.model.RssChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.internal.closeQuietly
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.nio.charset.Charset

internal class XmlParser(
    private val charset: Charset? = null,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend fun parseXML(input: ParserInput): RssChannel = withContext(dispatcher) {

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false

            val xmlPullParser = factory.newPullParser()

            // If the charset is null, then the parser will infer it from the feed
            xmlPullParser.setInput(input.inputStream, charset?.toString())

            var rssChannel: RssChannel? = null

            var eventType = xmlPullParser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xmlPullParser.contains(RssKeyword.Rss)) {
                        rssChannel = extractRSSContent(xmlPullParser)
                    } else if (xmlPullParser.contains(AtomKeyword.Feed.Atom)) {
                        rssChannel = extractAtomContent(xmlPullParser)
                    }
                }
                eventType = xmlPullParser.next()
            }

            return@withContext rssChannel
                ?: throw IllegalArgumentException(
                    "The provided XML is not supported. Only RSS and Atom feeds are supported",
                )
        } catch (exception: XmlPullParserException) {
            throw RssParsingException(
                message = "Something went wrong during the parsing of the feed. Please check if the XML is valid",
                cause = exception
            )
        } finally {
            input.inputStream.closeQuietly()
        }
    }
}
