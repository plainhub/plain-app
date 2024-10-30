package com.ismartcoding.lib.rss.internal

import com.ismartcoding.lib.rss.model.RssChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

internal fun CoroutineScope.extractAtomContent(
    xmlPullParser: XmlPullParser,
): RssChannel {
    val channelFactory = ChannelFactory()

    // A flag just to be sure of the correct parsing
    var insideItem = false
    var insideChannel = false

    var eventType = xmlPullParser.eventType

    // Start parsing the xml
    loop@ while (eventType != XmlPullParser.END_DOCUMENT && isActive) {

        // Start parsing the item
        when {
            eventType == XmlPullParser.START_TAG -> when {
                // Entering conditions
                xmlPullParser.contains(AtomKeyword.Feed.Atom) -> {
                    insideChannel = true
                }

                xmlPullParser.contains(AtomKeyword.Entry.Item) -> {
                    insideItem = true
                }
                //endregion

                //region Channel tags
                xmlPullParser.contains(AtomKeyword.Feed.Icon) -> {
                    if (insideChannel) {
                        channelFactory.channelImageBuilder.url(xmlPullParser.nextTrimmedText())
                    }
                }
                //endregion

                //region Item tags
                xmlPullParser.contains(AtomKeyword.Entry.Author) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.author(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(AtomKeyword.Entry.Category) -> {
                    if (insideItem) {
                        val nextText = xmlPullParser.nextTrimmedText()
                        val termAttributeValue = xmlPullParser.attributeValue(AtomKeyword.Entry.Term)

                        /**
                         * We want to look at the 'term' attribute and use that if no text is present
                         * such as `<category term="android"/>`
                         */
                        val categoryText = if (nextText?.isEmpty() == true) {
                            termAttributeValue
                        } else {
                            nextText
                        }
                        channelFactory.articleBuilder.addCategory(categoryText)
                    }
                }

                xmlPullParser.contains(AtomKeyword.Entry.Guid) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.guid(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(AtomKeyword.Entry.Content) -> {
                    if (insideItem) {
                        val content = try {
                            xmlPullParser.nextTrimmedText()
                        } catch (e: XmlPullParserException) {
                            // If there's some html not escaped, the parsing is going to fail
                            null
                        }
                        channelFactory.articleBuilder.content(content)
                        channelFactory.setImageFromContent(content)
                    }
                }

                xmlPullParser.contains(AtomKeyword.Feed.Updated) -> {
                    when {
                        insideItem -> {
                            channelFactory.articleBuilder.pubDateIfNull(xmlPullParser.nextTrimmedText())
                        }

                        insideChannel -> {
                            channelFactory.channelBuilder.lastBuildDate(xmlPullParser.nextTrimmedText())
                        }
                    }
                }

                xmlPullParser.contains(AtomKeyword.Entry.Published) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.pubDateIfNull(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(AtomKeyword.Feed.Subtitle) -> {
                    if (insideChannel) {
                        channelFactory.channelBuilder.description(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(AtomKeyword.Entry.Description) -> {
                    if (insideItem) {
                        val description = xmlPullParser.nextTrimmedText()
                        channelFactory.articleBuilder.description(description)
                        channelFactory.setImageFromContent(description)
                    }
                }
                //region Mixed tags
                xmlPullParser.contains(AtomKeyword.Feed.Title) -> {
                    when {
                        insideItem -> channelFactory.articleBuilder.title(xmlPullParser.nextTrimmedText())
                        insideChannel -> channelFactory.channelBuilder.title(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(AtomKeyword.Feed.Link) -> {
                    if (insideChannel) {
                        val href = xmlPullParser.attributeValue(
                            AtomKeyword.Link.Href
                        )
                        val rel = xmlPullParser.attributeValue(
                            AtomKeyword.Link.Rel
                        )
                        if (rel != AtomKeyword.Link.Edit && rel != AtomKeyword.Link.Self) {
                            when {
                                insideItem -> channelFactory.articleBuilder.link(href)
                                else -> channelFactory.channelBuilder.link(href)
                            }
                        }
                    }
                }
            }

            // Exit conditions
            eventType == XmlPullParser.END_TAG && xmlPullParser.contains(AtomKeyword.Entry.Item) -> {
                // The item is correctly parsed
                insideItem = false
                channelFactory.buildArticle()
            }

            eventType == XmlPullParser.END_TAG && xmlPullParser.contains(AtomKeyword.Feed.Atom) -> {
                // The channel is correctly parsed
                insideChannel = false
            }
        }
        eventType = xmlPullParser.next()
    }
    return channelFactory.build()
}
