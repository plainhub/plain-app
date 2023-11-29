package com.ismartcoding.lib.rss.internal

import com.ismartcoding.lib.rss.model.RssChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import org.xmlpull.v1.XmlPullParser

internal fun CoroutineScope.extractRSSContent(
    xmlPullParser: XmlPullParser,
): RssChannel {
    val channelFactory = ChannelFactory()

    // A flag just to be sure of the correct parsing
    var insideItem = false
    var insideChannel = false
    var insideChannelImage = false
    var insideItunesOwner = false

    var eventType = xmlPullParser.eventType

    // Start parsing the xml
    loop@ while (eventType != XmlPullParser.END_DOCUMENT && isActive) {

        // Start parsing the item
        when {
            eventType == XmlPullParser.START_TAG -> when {
                // Entering conditions
                xmlPullParser.contains(RssKeyword.Channel.Channel) -> {
                    insideChannel = true
                }

                xmlPullParser.contains(RssKeyword.Item.Item) -> {
                    insideItem = true
                }

                xmlPullParser.contains(RssKeyword.Channel.Itunes.Owner) -> {
                    insideItunesOwner = true
                }

                //region Channel tags
                xmlPullParser.contains(RssKeyword.Channel.LastBuildDate) -> {
                    if (insideChannel) {
                        channelFactory.channelBuilder.lastBuildDate(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Channel.UpdatePeriod) -> {
                    if (insideChannel) {
                        channelFactory.channelBuilder.updatePeriod(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Url) -> {
                    if (insideChannelImage) {
                        channelFactory.channelImageBuilder.url(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Channel.Itunes.Category) -> {
                    if (insideChannel) {
                        val category = xmlPullParser.attributeValue(RssKeyword.Channel.Itunes.Text)
                        channelFactory.itunesChannelBuilder.addCategory(category)
                    }
                }

                xmlPullParser.contains(RssKeyword.Channel.Itunes.Type) -> {
                    if (insideChannel) {
                        channelFactory.itunesChannelBuilder.type(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Channel.Itunes.NewFeedUrl) -> {
                    if (insideChannel) {
                        channelFactory.itunesChannelBuilder.newsFeedUrl(xmlPullParser.nextTrimmedText())
                    }
                }
                //endregion

                //region Item tags
                xmlPullParser.contains(RssKeyword.Item.Author) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.author(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Category) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.addCategory(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Thumbnail) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.image(
                            xmlPullParser.attributeValue(RssKeyword.Url)
                        )
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.MediaContent) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.image(
                            xmlPullParser.attributeValue(
                                RssKeyword.Url
                            )
                        )
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Enclosure) -> {
                    if (insideItem) {
                        val type = xmlPullParser.attributeValue(RssKeyword.Item.Type)
                        when {
                            type != null && type.contains("image") -> {
                                // If there are multiple elements, we take only the first
                                channelFactory.articleBuilder.image(
                                    xmlPullParser.attributeValue(
                                        RssKeyword.Url
                                    )
                                )
                            }

                            type != null && type.contains("audio") -> {
                                // If there are multiple elements, we take only the first
                                channelFactory.articleBuilder.audioIfNull(
                                    xmlPullParser.attributeValue(
                                        RssKeyword.Url
                                    )
                                )
                            }

                            type != null && type.contains("video") -> {
                                // If there are multiple elements, we take only the first
                                channelFactory.articleBuilder.videoIfNull(
                                    xmlPullParser.attributeValue(
                                        RssKeyword.Url
                                    )
                                )
                            }

                            else -> channelFactory.articleBuilder.image(
                                xmlPullParser.nextText().trim()
                            )
                        }
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Source) -> {
                    if (insideItem) {
                        val sourceUrl = xmlPullParser.attributeValue(RssKeyword.Url)
                        val sourceName = xmlPullParser.nextText()
                        channelFactory.articleBuilder.sourceName(sourceName)
                        channelFactory.articleBuilder.sourceUrl(sourceUrl)
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Time) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.pubDate(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Guid) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.guid(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Content) -> {
                    if (insideItem) {
                        val content = xmlPullParser.nextTrimmedText()
                        channelFactory.articleBuilder.content(content)
                        channelFactory.setImageFromContent(content)
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.PubDate) -> {
                    if (insideItem) {
                        val nextTokenType = xmlPullParser.next()
                        if (nextTokenType == XmlPullParser.TEXT) {
                            channelFactory.articleBuilder.pubDate(xmlPullParser.text.trim())
                        }
                        // Skip to be able to find date inside 'tag' tag
                        continue@loop
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.News.Image) -> {
                    if (insideItem) {
                        channelFactory.articleBuilder.image(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Itunes.Episode) -> {
                    if (insideItem) {
                        channelFactory.itunesArticleBuilder.episode(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Itunes.EpisodeType) -> {
                    if (insideItem) {
                        channelFactory.itunesArticleBuilder.episodeType(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Itunes.Season) -> {
                    if (insideItem) {
                        channelFactory.itunesArticleBuilder.season(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Comments) -> {
                    if (insideItem) {
                        val url = xmlPullParser.nextTrimmedText()
                        channelFactory.articleBuilder.commentUrl(url)
                    }
                }

                xmlPullParser.contains(RssKeyword.Item.Thumb) -> {
                    if (insideItem) {
                        val imageUrl = xmlPullParser.nextTrimmedText()
                        channelFactory.articleBuilder.image(imageUrl)
                    }
                }
                //endregion

                //region Itunes Owner tags
                xmlPullParser.contains(RssKeyword.Channel.Itunes.OwnerName) -> {
                    if (insideItunesOwner) {
                        channelFactory.itunesOwnerBuilder.name(xmlPullParser.nextTrimmedText())
                    }
                }

                xmlPullParser.contains(RssKeyword.Channel.Itunes.OwnerEmail) -> {
                    if (insideItunesOwner) {
                        channelFactory.itunesOwnerBuilder.email(xmlPullParser.nextTrimmedText())
                    }
                }
                //endregion

                //region Mixed tags
                xmlPullParser.contains(RssKeyword.Image) -> when {
                    insideChannel && !insideItem -> insideChannelImage = true
                    insideItem -> {
                        xmlPullParser.next()
                        val text = xmlPullParser.text?.trim()
                        // Get the image text if it's not contained in another tag
                        if (!text.isNullOrEmpty()) {
                            channelFactory.articleBuilder.image(text)
                        } else {
                            xmlPullParser.next()
                            if (xmlPullParser.contains(RssKeyword.Link)) {
                                channelFactory.articleBuilder.image(xmlPullParser.nextTrimmedText())
                            }
                        }
                    }
                }

                xmlPullParser.contains(RssKeyword.Title) -> {
                    if (insideChannel) {
                        when {
                            insideChannelImage -> {
                                channelFactory.channelImageBuilder.title(xmlPullParser.nextTrimmedText())
                            }

                            insideItem -> channelFactory.articleBuilder.title(xmlPullParser.nextTrimmedText())
                            else -> channelFactory.channelBuilder.title(xmlPullParser.nextTrimmedText())
                        }
                    }
                }

                xmlPullParser.contains(RssKeyword.Link) -> {
                    if (insideChannel) {
                        when {
                            insideChannelImage -> {
                                channelFactory.channelImageBuilder.link(xmlPullParser.nextTrimmedText())
                            }

                            insideItem -> channelFactory.articleBuilder.link(xmlPullParser.nextTrimmedText())
                            else -> channelFactory.channelBuilder.link(xmlPullParser.nextTrimmedText())
                        }
                    }
                }

                xmlPullParser.contains(RssKeyword.Description) -> {
                    if (insideChannel) {
                        when {
                            insideItem -> {
                                val description = xmlPullParser.nextTrimmedText()
                                channelFactory.articleBuilder.description(description)
                                channelFactory.setImageFromContent(description)
                            }

                            insideChannelImage -> {
                                channelFactory.channelImageBuilder.description(xmlPullParser.nextTrimmedText())
                            }

                            else -> channelFactory.channelBuilder.description(xmlPullParser.nextTrimmedText())
                        }
                    }
                }

                xmlPullParser.contains(RssKeyword.Itunes.Author) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.author(xmlPullParser.nextTrimmedText())
                    insideChannel -> channelFactory.itunesChannelBuilder.author(xmlPullParser.nextTrimmedText())
                }

                xmlPullParser.contains(RssKeyword.Itunes.Duration) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.duration(xmlPullParser.nextTrimmedText())
                    insideChannel -> channelFactory.itunesChannelBuilder.duration(xmlPullParser.nextTrimmedText())
                }

                xmlPullParser.contains(RssKeyword.Itunes.Keywords) -> {
                    val keywords = xmlPullParser.nextTrimmedText()
                    when {
                        insideItem -> channelFactory.setArticleItunesKeywords(keywords)
                        insideChannel -> channelFactory.setChannelItunesKeywords(keywords)
                    }
                }

                xmlPullParser.contains(RssKeyword.Itunes.Image) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.image(
                        xmlPullParser.attributeValue(
                            RssKeyword.Href
                        )
                    )

                    insideChannel -> channelFactory.itunesChannelBuilder.image(
                        xmlPullParser.attributeValue(
                            RssKeyword.Href
                        )
                    )
                }

                xmlPullParser.contains(RssKeyword.Itunes.Explicit) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.explicit(xmlPullParser.nextTrimmedText())
                    insideChannel -> channelFactory.itunesChannelBuilder.explicit(xmlPullParser.nextTrimmedText())
                }

                xmlPullParser.contains(RssKeyword.Itunes.Subtitle) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.subtitle(xmlPullParser.nextTrimmedText())
                    insideChannel -> channelFactory.itunesChannelBuilder.subtitle(xmlPullParser.nextTrimmedText())
                }

                xmlPullParser.contains(RssKeyword.Itunes.Summary) -> when {
                    insideItem -> channelFactory.itunesArticleBuilder.summary(xmlPullParser.nextTrimmedText())
                    insideChannel -> channelFactory.itunesChannelBuilder.summary(xmlPullParser.nextTrimmedText())
                }
                //endregion
            }

            // Exit conditions
            eventType == XmlPullParser.END_TAG && xmlPullParser.contains(RssKeyword.Item.Item) -> {
                // The item is correctly parsed
                insideItem = false
                // Set data
                channelFactory.buildArticle()
            }

            eventType == XmlPullParser.END_TAG && xmlPullParser.contains(RssKeyword.Channel.Channel) -> {
                // The channel is correctly parsed
                insideChannel = false
            }

            eventType == XmlPullParser.END_TAG && xmlPullParser.contains(RssKeyword.Image) -> {
                // The channel image is correctly parsed
                insideChannelImage = false
            }

            eventType == XmlPullParser.END_TAG && xmlPullParser.contains(RssKeyword.Channel.Itunes.Owner) -> {
                // The itunes owner is correctly parsed
                channelFactory.buildItunesOwner()
                insideItunesOwner = false
            }
        }
        eventType = xmlPullParser.next()
    }

    return channelFactory.build()
}
