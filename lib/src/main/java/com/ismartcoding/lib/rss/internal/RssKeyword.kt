package com.ismartcoding.lib.rss.internal

internal class RssKeyword {
    companion object {
        val Rss = "rss"
        val Title = "title"
        val Image = "image"
        val Link = "link"
        val Href = "href"
        val Url = "url"
        val Description = "description"
    }

    object Itunes {
        val Author = "itunes:author"
        val Duration = "itunes:duration"
        val Keywords = "itunes:keywords"
        val Image = "itunes:image"
        val Explicit = "itunes:explicit"
        val Subtitle = "itunes:subtitle"
        val Summary = "itunes:summary"
    }

    object Channel {
        val Channel = "channel"
        val UpdatePeriod = "sy:updatePeriod"
        val LastBuildDate = "lastBuildDate"

        object Itunes {
            val Category = "itunes:category"
            val Owner = "itunes:owner"
            val OwnerName = "itunes:name"
            val OwnerEmail = "itunes:email"
            val Type = "itunes:type"
            val NewFeedUrl = "itunes:new-feed-url"
            val Text = "text"
        }
    }

    object Item {
        val Item = "item"
        val Author = "dc:creator"
        val Category = "category"
        val MediaContent = "media:content"
        val Enclosure = "enclosure"
        val Content = "content:encoded"
        val PubDate = "pubDate"
        val Time = "time"
        val Type = "type"
        val Guid = "guid"
        val Source = "source"
        val Thumbnail = "media:thumbnail"
        val Comments = "comments"
        val Thumb = "thumb"

        object News {
            val Image = "News:Image"
        }

        object Itunes {
            val Episode = "itunes:episode"
            val Season = "itunes:season"
            val EpisodeType = "itunes:episodeType"
        }
    }
}
