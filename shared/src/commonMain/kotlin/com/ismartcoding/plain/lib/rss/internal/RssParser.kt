package com.ismartcoding.plain.lib.rss.internal

import com.ismartcoding.plain.lib.opml.SimpleXmlReader
import com.ismartcoding.plain.lib.rss.model.RssChannel

internal fun extractRSSContent(
    reader: SimpleXmlReader,
): RssChannel {
    val channelFactory = ChannelFactory()

    var insideItem = false
    var insideChannel = false
    var insideChannelImage = false
    var insideItunesOwner = false

    var eventType = reader.eventType

    loop@ while (eventType != SimpleXmlReader.END_DOCUMENT) {
        when {
            eventType == SimpleXmlReader.START_TAG -> when {
                reader.contains(RssKeyword.Channel.Channel) -> {
                    insideChannel = true
                }

                reader.contains(RssKeyword.Item.Item) -> {
                    insideItem = true
                }

                reader.contains(RssKeyword.Channel.Itunes.Owner) -> {
                    insideItunesOwner = true
                }

                reader.contains(RssKeyword.Channel.LastBuildDate) -> {
                    if (insideChannel) {
                        channelFactory.channelBuilder.lastBuildDate(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Channel.UpdatePeriod) -> {
                    if (insideChannel) {
                        channelFactory.channelBuilder.updatePeriod(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Url) -> {
                    if (insideChannelImage) {
                        channelFactory.channelImageBuilder.url(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Channel.Itunes.Category) -> {
                    if (insideChannel) {
                        val category = reader.attributeValue(RssKeyword.Channel.Itunes.Text)
                        channelFactory.itunesChannelBuilder.addCategory(category)
                    }
                }

                reader.contains(RssKeyword.Channel.Itunes.Type) -> {
                    if (insideChannel) {
                        channelFactory.itunesChannelBuilder.type(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Channel.Itunes.NewFeedUrl) -> {
                    if (insideChannel) {
                        channelFactory.itunesChannelBuilder.newsFeedUrl(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Item.Author) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.author(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Item.Category) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.addCategory(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Item.Thumbnail) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.image(
                            reader.attributeValue(RssKeyword.Url)
                        )
                    }
                }

                reader.contains(RssKeyword.Item.MediaContent) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.image(
                            reader.attributeValue(RssKeyword.Url)
                        )
                    }
                }

                reader.contains(RssKeyword.Item.Enclosure) -> {
                    if (insideItem) {
                        val type = reader.attributeValue(RssKeyword.Item.Type)
                        when {
                            type != null && type.contains("image") -> {
                                channelFactory.articleBuilder.image(
                                    reader.attributeValue(RssKeyword.Url)
                                )
                            }

                            type != null && type.contains("audio") -> {
                                channelFactory.articleBuilder.audioIfNull(
                                    reader.attributeValue(RssKeyword.Url)
                                )
                            }

                            type != null && type.contains("video") -> {
                                channelFactory.articleBuilder.videoIfNull(
                                    reader.attributeValue(RssKeyword.Url)
                                )
                            }

                            else -> channelFactory.articleBuilder.image(
                                reader.nextText().trim()
                            )
                        }
                    }
                }

                reader.contains(RssKeyword.Item.Source) -> {
                    if (insideItem) {
                        val sourceUrl = reader.attributeValue(RssKeyword.Url)
                        val sourceName = reader.nextText()
                        channelFactory.articleBuilder.sourceName(sourceName)
                        channelFactory.articleBuilder.sourceUrl(sourceUrl)
                    }
                }

                reader.contains(RssKeyword.Item.Time) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.pubDate(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Item.Guid) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.guid(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Item.Content) -> {
                    if (insideItem) {
                        val content = reader.nextTrimmedText()
                        channelFactory.articleBuilder.content(content)
                        channelFactory.setImageFromContent(content)
                    }
                }

                reader.contains(RssKeyword.Item.PubDate) -> {
                    if (insideItem) {
                        val nextTokenType = reader.next()
                        if (nextTokenType == SimpleXmlReader.TEXT) {
                            channelFactory.articleBuilder.pubDate(reader.text.trim())
                        }
                        continue@loop
                    }
                }

                reader.contains(RssKeyword.Item.News.Image) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.image(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Item.Itunes.Episode) -> {
                    if (insideItem) {
                        channelFactory.itunesArticleBuilder.episode(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Item.Itunes.EpisodeType) -> {
                    if (insideItem) {
                        channelFactory.itunesArticleBuilder.episodeType(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Item.Itunes.Season) -> {
                    if (insideItem) {
                        channelFactory.itunesArticleBuilder.season(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Item.Comments) -> {
                    if (insideItem) {
                        val url = reader.nextTrimmedText()
                        channelFactory.articleBuilder.commentUrl(url)
                    }
                }

                reader.contains(RssKeyword.Item.Thumb) -> {
                    if (insideItem) {
                        val imageUrl = reader.nextTrimmedText()
                        channelFactory.articleBuilder.image(imageUrl)
                    }
                }

                reader.contains(RssKeyword.Channel.Itunes.OwnerName) -> {
                    if (insideItunesOwner) {
                        channelFactory.itunesOwnerBuilder.name(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Channel.Itunes.OwnerEmail) -> {
                    if (insideItunesOwner) {
                        channelFactory.itunesOwnerBuilder.email(reader.nextTrimmedText())
                    }
                }

                reader.contains(RssKeyword.Image) -> when {
                    insideChannel && !insideItem -> insideChannelImage = true
                    insideItem -> {
                        reader.next()
                        val text = reader.text.trim()
                        if (text.isNotEmpty()) {
                            channelFactory.articleBuilder.image(text)
                        } else {
                            reader.next()
                            if (reader.contains(RssKeyword.Link)) {
                                channelFactory.articleBuilder.image(reader.nextTrimmedText())
                            }
                        }
                    }
                }

                reader.contains(RssKeyword.Title) -> {
                    if (insideChannel) {
                        when {
                            insideChannelImage -> {
                                channelFactory.channelImageBuilder.title(reader.nextTrimmedText())
                            }
                            insideItem -> channelFactory.articleBuilder.title(reader.nextTrimmedText())
                            else -> channelFactory.channelBuilder.title(reader.nextTrimmedText())
                        }
                    }
                }

                reader.contains(RssKeyword.Link) -> {
                    if (insideChannel) {
                        when {
                            insideChannelImage -> {
                                channelFactory.channelImageBuilder.link(reader.nextTrimmedText())
                            }
                            insideItem -> channelFactory.articleBuilder.link(reader.nextTrimmedText())
                            else -> channelFactory.channelBuilder.link(reader.nextTrimmedText())
                        }
                    }
                }

                reader.contains(RssKeyword.Description) -> {
                    if (insideChannel) {
                        when {
                            insideItem -> {
                                val description = reader.nextTrimmedText()
                                channelFactory.articleBuilder.description(description)
                                channelFactory.setImageFromContent(description)
                            }

                            insideChannelImage -> {
                                channelFactory.channelImageBuilder.description(reader.nextTrimmedText())
                            }

                            else -> channelFactory.channelBuilder.description(reader.nextTrimmedText())
                        }
                    }
                }

                reader.contains(RssKeyword.Itunes.Author) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.author(reader.nextTrimmedText())
                    insideChannel -> channelFactory.itunesChannelBuilder.author(reader.nextTrimmedText())
                }

                reader.contains(RssKeyword.Itunes.Duration) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.duration(reader.nextTrimmedText())
                    insideChannel -> channelFactory.itunesChannelBuilder.duration(reader.nextTrimmedText())
                }

                reader.contains(RssKeyword.Itunes.Keywords) -> {
                    val keywords = reader.nextTrimmedText()
                    when {
                        insideItem -> channelFactory.setArticleItunesKeywords(keywords)
                        insideChannel -> channelFactory.setChannelItunesKeywords(keywords)
                    }
                }

                reader.contains(RssKeyword.Itunes.Image) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.image(
                        reader.attributeValue(RssKeyword.Href)
                    )
                    insideChannel -> channelFactory.itunesChannelBuilder.image(
                        reader.attributeValue(RssKeyword.Href)
                    )
                }

                reader.contains(RssKeyword.Itunes.Explicit) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.explicit(reader.nextTrimmedText())
                    insideChannel -> channelFactory.itunesChannelBuilder.explicit(reader.nextTrimmedText())
                }

                reader.contains(RssKeyword.Itunes.Subtitle) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.subtitle(reader.nextTrimmedText())
                    insideChannel -> channelFactory.itunesChannelBuilder.subtitle(reader.nextTrimmedText())
                }

                reader.contains(RssKeyword.Itunes.Summary) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.summary(reader.nextTrimmedText())
                    insideChannel -> channelFactory.itunesChannelBuilder.summary(reader.nextTrimmedText())
                }
            }

            eventType == SimpleXmlReader.END_TAG && reader.contains(RssKeyword.Item.Item) -> {
                insideItem = false
                channelFactory.buildArticle()
            }

            eventType == SimpleXmlReader.END_TAG && reader.contains(RssKeyword.Channel.Channel) -> {
                insideChannel = false
            }

            eventType == SimpleXmlReader.END_TAG && reader.contains(RssKeyword.Image) -> {
                insideChannelImage = false
            }

            eventType == SimpleXmlReader.END_TAG && reader.contains(RssKeyword.Channel.Itunes.Owner) -> {
                channelFactory.buildItunesOwner()
                insideItunesOwner = false
            }
        }
        eventType = reader.next()
    }

    return channelFactory.build()
}
