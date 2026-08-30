package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.HtmlUtils
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.html2md.MDConverter
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
