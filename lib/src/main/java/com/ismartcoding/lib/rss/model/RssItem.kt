package com.ismartcoding.lib.rss.model

data class RssItem(
    val guid: String?,
    val title: String?,
    val author: String?,
    val link: String?,
    val pubDate: String?,
    val description: String?,
    val content: String?,
    val image: String?,
    val audio: String?,
    val video: String?,
    val sourceName: String?,
    val sourceUrl: String?,
    val categories: List<String>,
    val itunesItemData: ItunesItemData?,
    val commentsUrl: String?,
) {
    internal data class Builder(
        private var guid: String? = null,
        private var title: String? = null,
        private var author: String? = null,
        private var link: String? = null,
        private var pubDate: String? = null,
        private var description: String? = null,
        private var content: String? = null,
        private var image: String? = null,
        private var audio: String? = null,
        private var video: String? = null,
        private var sourceName: String? = null,
        private var sourceUrl: String? = null,
        private val categories: MutableList<String> = mutableListOf(),
        private var itunesItemData: ItunesItemData? = null,
        private var commentUrl: String? = null,
    ) {
        fun guid(guid: String?) = apply { this.guid = guid }
        fun title(title: String?) = apply { this.title = title }
        fun author(author: String?) = apply { this.author = author }
        fun link(link: String?) = apply { this.link = link }
        fun pubDate(pubDate: String?) = apply {
            this.pubDate = pubDate
        }

        fun pubDateIfNull(pubDate: String?) = apply {
            if (this.pubDate == null) {
                this.pubDate = pubDate
            }
        }

        fun description(description: String?) = apply { this.description = description }
        fun content(content: String?) = apply { this.content = content }
        fun image(image: String?) = apply {
            if (this.image == null && image?.isNotEmpty() == true) {
                this.image = image
            }
        }

        fun audio(audio: String?) = apply { this.audio = audio }
        fun audioIfNull(audio: String?) = apply {
            if (this.audio == null) {
                this.audio = audio
            }
        }

        fun video(video: String?) = apply { this.video = video }
        fun videoIfNull(video: String?) = apply {
            if (this.video == null) {
                this.video = video
            }
        }

        fun sourceName(sourceName: String?) = apply { this.sourceName = sourceName }
        fun sourceUrl(sourceUrl: String?) = apply { this.sourceUrl = sourceUrl }
        fun addCategory(category: String?) = apply {
            if (category != null) {
                this.categories.add(category)
            }
        }

        fun itunesArticleData(itunesItemData: ItunesItemData?) =
            apply { this.itunesItemData = itunesItemData }

        fun commentUrl(url: String?) = apply { this.commentUrl = url }

        fun build() = RssItem(
            guid = guid,
            title = title,
            author = author,
            link = link,
            pubDate = pubDate,
            description = description,
            content = content,
            image = image,
            audio = audio,
            video = video,
            sourceName = sourceName,
            sourceUrl = sourceUrl,
            categories = categories,
            itunesItemData = itunesItemData,
            commentsUrl = commentUrl,
        )
    }
}
