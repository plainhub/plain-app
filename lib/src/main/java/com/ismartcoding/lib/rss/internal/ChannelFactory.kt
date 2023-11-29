package com.ismartcoding.lib.rss.internal

import com.ismartcoding.lib.rss.model.ItunesChannelData
import com.ismartcoding.lib.rss.model.ItunesItemData
import com.ismartcoding.lib.rss.model.ItunesOwner
import com.ismartcoding.lib.rss.model.RssChannel
import com.ismartcoding.lib.rss.model.RssImage
import com.ismartcoding.lib.rss.model.RssItem


internal class ChannelFactory {
    val channelBuilder = RssChannel.Builder()
    val channelImageBuilder = RssImage.Builder()
    var articleBuilder = RssItem.Builder()
    val itunesChannelBuilder = ItunesChannelData.Builder()
    var itunesArticleBuilder = ItunesItemData.Builder()
    var itunesOwnerBuilder = ItunesOwner.Builder()

    // This image url is extracted from the content and the description of the rss item.
    // It's a fallback just in case there aren't any images in the enclosure tag.
    private var imageUrlFromContent: String? = null

    fun buildArticle() {
        articleBuilder.image(imageUrlFromContent)
        articleBuilder.itunesArticleData(itunesArticleBuilder.build())
        channelBuilder.addItem(articleBuilder.build())
        // Reset temp data
        imageUrlFromContent = null
        articleBuilder = RssItem.Builder()
        itunesArticleBuilder = ItunesItemData.Builder()
    }

    fun buildItunesOwner() {
        itunesChannelBuilder.owner(itunesOwnerBuilder.build())
        itunesOwnerBuilder = ItunesOwner.Builder()
    }

    /**
     * Finds the first img tag and gets the src as the featured image.
     *
     * @param content The content in which to search for the tag
     * @return The url, if there is one
     */
    fun setImageFromContent(content: String?) {
        try {
            val urlRegex = Regex(pattern = "https?:\\/\\/[^\\s<>\"]+\\.(?:jpg|jpeg|png|gif|bmp|webp)")
            content
                ?.let{ urlRegex.find(it) }
                ?.let {
                    it.value.trim().let { imgUrl ->
                        if (!imgUrl.contains(EMOJI_WEBSITE)) {
                            imageUrlFromContent = imgUrl
                        }
                    }
                }
        } catch (e: Throwable) {
            // Do nothing, on iOS it could fail for too much recursion
        }
    }

    fun setChannelItunesKeywords(keywords: String?) {
        val keywordList = extractItunesKeywords(keywords)
        if (keywordList.isNotEmpty()) {
            itunesChannelBuilder.keywords(keywordList)
        }
    }

    fun setArticleItunesKeywords(keywords: String?) {
        val keywordList = extractItunesKeywords(keywords)
        if (keywordList.isNotEmpty()) {
            itunesArticleBuilder.keywords(keywordList)
        }
    }

    private fun extractItunesKeywords(keywords: String?): List<String> =
        keywords?.split(",")?.mapNotNull {
            it.ifEmpty {
                null
            }
        } ?: emptyList()


    fun build(): RssChannel {
        val channelImage = channelImageBuilder.build()
        if (channelImage.isNotEmpty()) {
            channelBuilder.image(channelImage)
        }
        channelBuilder.itunesChannelData(itunesChannelBuilder.build())
        return channelBuilder.build()
    }

    private companion object {
        const val EMOJI_WEBSITE = "https://s.w.org/images/core/emoji"
    }
}
