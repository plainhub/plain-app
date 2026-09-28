package com.ismartcoding.plain.lib.rss.internal

import com.ismartcoding.plain.lib.opml.SimpleXmlReader
import com.ismartcoding.plain.lib.rss.model.RssChannel

internal fun extractAtomContent(
    reader: SimpleXmlReader,
): RssChannel {
    val channelFactory = ChannelFactory()

    var insideItem = false
    var insideChannel = false

    var eventType = reader.eventType

    while (eventType != SimpleXmlReader.END_DOCUMENT) {
        when {
            eventType == SimpleXmlReader.START_TAG -> when {
                reader.contains(AtomKeyword.Feed.Atom) -> {
                    insideChannel = true
                }

                reader.contains(AtomKeyword.Entry.Item) -> {
                    insideItem = true
                }

                reader.contains(AtomKeyword.Feed.Icon) -> {
                    if (insideChannel) {
                        channelFactory.channelImageBuilder.url(reader.nextTrimmedText())
                    }
                }

                reader.contains(AtomKeyword.Entry.Author) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.author(reader.nextTrimmedText())
                    }
                }

                reader.contains(AtomKeyword.Entry.Category) -> {
                    if (insideItem) {
                        val nextText = reader.nextTrimmedText()
                        val termAttributeValue = reader.attributeValue(AtomKeyword.Entry.Term)
                        val categoryText = if (nextText?.isEmpty() == true) {
                            termAttributeValue
                        } else {
                            nextText
                        }
                        channelFactory.articleBuilder.addCategory(categoryText)
                    }
                }

                reader.contains(AtomKeyword.Entry.Guid) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.guid(reader.nextTrimmedText())
                    }
                }

                reader.contains(AtomKeyword.Entry.Content) -> {
                    if (insideItem) {
                        val content = try {
                            reader.nextTrimmedText()
                        } catch (e: Exception) {
                            null
                        }
                        channelFactory.articleBuilder.content(content)
                        channelFactory.setImageFromContent(content)
                    }
                }

                reader.contains(AtomKeyword.Feed.Updated) -> {
                    when {
                        insideItem -> {
                            channelFactory.articleBuilder.pubDateIfNull(reader.nextTrimmedText())
                        }
                        insideChannel -> {
                            channelFactory.channelBuilder.lastBuildDate(reader.nextTrimmedText())
                        }
                    }
                }

                reader.contains(AtomKeyword.Entry.Published) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.pubDateIfNull(reader.nextTrimmedText())
                    }
                }

                reader.contains(AtomKeyword.Feed.Subtitle) -> {
                    if (insideChannel) {
                        channelFactory.channelBuilder.description(reader.nextTrimmedText())
                    }
                }

                reader.contains(AtomKeyword.Entry.Description) -> {
                    if (insideItem) {
                        val description = reader.nextTrimmedText()
                        channelFactory.articleBuilder.description(description)
                        channelFactory.setImageFromContent(description)
                    }
                }

                reader.contains(AtomKeyword.Feed.Title) -> {
                    when {
                        insideItem -> channelFactory.articleBuilder.title(reader.nextTrimmedText())
                        insideChannel -> channelFactory.channelBuilder.title(reader.nextTrimmedText())
                    }
                }

                reader.contains(AtomKeyword.Feed.Link) -> {
                    if (insideChannel) {
                        val href = reader.attributeValue(AtomKeyword.Link.Href)
                        val rel = reader.attributeValue(AtomKeyword.Link.Rel)
                        if (rel != AtomKeyword.Link.Edit && rel != AtomKeyword.Link.Self) {
                            when {
                                insideItem -> channelFactory.articleBuilder.link(href)
                                else -> channelFactory.channelBuilder.link(href)
                            }
                        }
                    }
                }
            }

            eventType == SimpleXmlReader.END_TAG && reader.contains(AtomKeyword.Entry.Item) -> {
                insideItem = false
                channelFactory.buildArticle()
            }

            eventType == SimpleXmlReader.END_TAG && reader.contains(AtomKeyword.Feed.Atom) -> {
                insideChannel = false
            }
        }
        eventType = reader.next()
    }
    return channelFactory.build()
}
