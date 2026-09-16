package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.feed.HtmlUtils
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.html2md.MDConverter
import com.ismartcoding.plain.lib.rss.model.RssChannel
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.readability4j.Readability4J

suspend fun DFeedEntry.fetchContentAsync(): ApiResult = withIO {
    try {
        val httpClient = createBrowserHttpClient()
        val response = httpClient.get(url)

        if (response.isOk()) {
            val input = response.bodyAsText()
            Readability4J.parse(url, input).articleContent?.let { articleContent ->
                articleContent.selectFirst("h1")?.remove()
                val c = articleContent.toString()
                val mobilizedHtml = HtmlUtils.improveHtmlContent(c, HtmlUtils.getBaseUrl(url))
                val summary = getSummary()
                if (summary.isEmpty() || c.length >= summary.length) {
                    val imagesList = HtmlUtils.getImageURLs(mobilizedHtml)
                    if (imagesList.isNotEmpty()) {
                        if (image.isEmpty()) {
                            image = HtmlUtils.getMainImageURL(imagesList)
                        }
                    }

                    if (image.isNotEmpty() && !image.startsWith("/") && !image.startsWith("fid:", true)) {
                        try {
                            val r = httpClient.get(image)
                            r.use {
                                if (it.isOk()) {
                                    val imageBytes = it.bodyAsBytes()
                                    val contentType = it.header("Content-Type")?.lowercase() ?: ""
                                    val fidUri = importImageBytesToFid(imageBytes, contentType)
                                    if (fidUri != null) {
                                        image = fidUri
                                    }
                                }
                            }
                        } catch (ex: Exception) {
                            LogCat.e(ex.toString())
                        }
                    }
                    val md = MDConverter().convert(mobilizedHtml)
                    if (md.length >= description.length) {
                        content = md
                    } else if (content.isEmpty()) {
                        content = description
                    }
                    updatedAt = TimeHelper.now()
                    FeedEntryHelper.updateAsync(this@fetchContentAsync)
                }
            }
        }

        response.close()
        return@withIO ApiResult(response)
    } catch (ex: Throwable) {
        LogCat.e("fetchContentAsync: ${ex.message}")
        return@withIO ApiResult(null, ex)
    }
}

private const val MAX_LOGO_SIZE = 2 * 1024 * 1024 // 2MB

private val linkTagRegex = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
private val relAttrRegex = Regex("rel\\s*=\\s*[\"']([^\"]*)[\"']", RegexOption.IGNORE_CASE)
private val hrefAttrRegex = Regex("href\\s*=\\s*[\"']([^\"]*)[\"']", RegexOption.IGNORE_CASE)
private val originRegex = Regex("^https?://[^/]+")

/**
 * Fetch and persist the feed's logo (one `fid:` URI in [DFeed.logo]).
 * Sources in priority order: RSS `<image><url>`, then favicon candidates from
 * the site's HTML (`apple-touch-icon` first, then `rel~=icon`). `.ico` files
 * are skipped because BitmapFactory cannot decode them. Failures are silent:
 * logo stays empty and the next sync retries.
 */
suspend fun fetchFeedLogoAsync(feed: DFeed, channel: RssChannel) = withIO {
    try {
        val siteUrl = channel.link?.takeIf { it.startsWith("http") } ?: feed.url
        val candidates = mutableListOf<String>()
        channel.image?.url?.let { resolveUrl(feed.url, it)?.let { u -> candidates.add(u) } }
        try {
            val client = createBrowserHttpClient()
            val response = client.get(siteUrl)
            response.use {
                if (it.isOk()) {
                    extractIconHrefs(it.bodyAsText()).forEach { href ->
                        resolveUrl(siteUrl, href)?.let { u -> candidates.add(u) }
                    }
                }
            }
        } catch (ex: Exception) {
            LogCat.e("fetchFeedLogoAsync html: ${ex.message}")
        }

        for (url in candidates) {
            if (url.substringBefore('?').lowercase().endsWith(".svg") ||
                url.substringBefore('?').lowercase().endsWith(".ico")
            ) {
                continue
            }
            try {
                val client = createBrowserHttpClient()
                val response = client.get(url)
                response.use {
                    if (it.isOk()) {
                        val contentType = it.header("Content-Type")?.lowercase() ?: ""
                        val decodable = listOf("png", "jpeg", "jpg", "webp").any { t -> contentType.contains(t) }
                        val bytes = if (decodable) it.bodyAsBytes() else ByteArray(0)
                        if (bytes.isNotEmpty() && bytes.size <= MAX_LOGO_SIZE) {
                            importImageBytesToFid(bytes, contentType)?.let { fid ->
                                FeedHelper.updateLogoAsync(feed.id, fid)
                                return@withIO
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                LogCat.e("fetchFeedLogoAsync: ${ex.message}")
            }
        }
    } catch (ex: Exception) {
        LogCat.e("fetchFeedLogoAsync: ${ex.message}")
    }
}

/** Resolve [ref] against an absolute [base] URL; returns null when impossible. */
private fun resolveUrl(base: String, ref: String): String? {
    if (ref.isEmpty() || ref.startsWith("data:")) return null
    if (ref.startsWith("http://") || ref.startsWith("https://")) return ref
    val origin = originRegex.find(base)?.value ?: return null
    return when {
        ref.startsWith("//") -> "https:$ref"
        ref.startsWith("/") -> origin + ref
        else -> "$origin/$ref"
    }
}

/** favicon candidates from `<link rel~=icon>` tags, apple-touch first. */
private fun extractIconHrefs(html: String): List<String> {
    return linkTagRegex.findAll(html)
        .mapNotNull { match ->
            val tag = match.value
            val rel = relAttrRegex.find(tag)?.groupValues?.get(1)?.lowercase() ?: return@mapNotNull null
            if (!rel.contains("icon")) return@mapNotNull null
            val href = hrefAttrRegex.find(tag)?.groupValues?.get(1) ?: return@mapNotNull null
            val score = when {
                rel.contains("apple-touch") -> 0
                href.contains("png") -> 1
                else -> 2
            }
            score to href
        }
        .sortedBy { it.first }
        .map { it.second }
        .toList()
}
