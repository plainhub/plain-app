package com.ismartcoding.plain.features.feed

import android.os.Environment
import androidx.core.text.HtmlCompat
import com.ismartcoding.lib.Readability4JPreprocessor
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.isOk
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.html2md.MDConverter
import com.ismartcoding.lib.rss.DateParser
import com.ismartcoding.lib.rss.model.RssItem
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.db.DFeedEntry
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.datetime.Instant
import net.dankito.readability4j.extended.Readability4JExtended
import net.dankito.readability4j.extended.processor.ArticleGrabberExtended
import net.dankito.readability4j.extended.util.RegExUtilExtended
import net.dankito.readability4j.model.ReadabilityOptions
import net.dankito.readability4j.processor.MetadataParser
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.*

fun RssItem.toDFeedEntry(
    feedId: String,
    feedUrl: String,
): DFeedEntry {
    val item = DFeedEntry()
    item.rawId =
        CryptoHelper.sha1(
            (feedId + "_" + (link ?: title ?: UUID.randomUUID().toString())).toByteArray(),
        )
    item.feedId = feedId
    if (title != null) {
        item.title = HtmlCompat.fromHtml(title!!, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().replace("\n", " ").trim()
    } else {
        item.title = ""
    }

    val feedBaseUrl = HtmlUtils.getBaseUrl(feedUrl)
    item.description = HtmlUtils.improveHtmlContent(content ?: description ?: "", feedBaseUrl)
    item.description = MDConverter().convert(item.description)
    item.url = link ?: ""

    item.image = image ?: ""

    item.author = author?.ifEmpty { sourceName ?: "" } ?: ""

    if (pubDate != null) {
        val date = DateParser.parseDate(pubDate!!, Locale.US)
        if (date != null && date.time < item.publishedAt.toEpochMilliseconds()) {
            item.publishedAt = Instant.fromEpochMilliseconds(date.time)
        }
    }

    return item
}

suspend fun DFeedEntry.fetchContentAsync(): ApiResult {
    try {
        val httpClient = HttpClientManager.browserClient()
        val response =
            httpClient.get(url) {
                headers {
                    set("accept", "*/*")
                }
            }

        if (response.isOk()) {
            val input = response.body<String>()
            val options = ReadabilityOptions()
            val regExUtil = RegExUtilExtended()
            Readability4JExtended(
                url,
                input,
                options = options,
                regExUtil = regExUtil,
                preprocessor = Readability4JPreprocessor(regExUtil),
                metadataParser = MetadataParser(regExUtil),
                articleGrabber = ArticleGrabberExtended(options, regExUtil),
            ).parse().articleContent?.let { articleContent ->
                articleContent.selectFirst("h1")?.remove()
                val c = articleContent.toString()
                val mobilizedHtml = HtmlUtils.improveHtmlContent(c, HtmlUtils.getBaseUrl(url))
                if (description.isEmpty() ||
                    HtmlCompat.fromHtml(mobilizedHtml, HtmlCompat.FROM_HTML_MODE_LEGACY).length > description.length
                ) { // If the retrieved text is smaller than the original one, then we certainly failed...
                    val imagesList = HtmlUtils.getImageURLs(mobilizedHtml)
                    if (imagesList.isNotEmpty()) {
                        if (image.isEmpty()) {
                            image = HtmlUtils.getMainImageURL(imagesList)
                        }
                    }

                    if (image.isNotEmpty() && !image.startsWith("/")) {
                        val r = httpClient.get(image)
                        if (r.isOk()) {
                            val dir = MainApp.instance.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.path + "/feeds"
                            File(dir).mkdirs()
                            var path = "$dir/main-${CryptoHelper.sha1(image.toByteArray())}"
                            val extension = image.getFilenameExtension()
                            if (extension.isNotEmpty()) {
                                path += ".$extension"
                            }
                            val file = File(path)
                            file.createNewFile()
                            r.bodyAsChannel().copyAndClose(file.writeChannel())
                            image = path
                        }
                    }
                    content = MDConverter().convert(mobilizedHtml)

                    FeedEntryHelper.updateAsync(id) {
                        this.image = this@fetchContentAsync.image
                        this.content = this@fetchContentAsync.content
                    }
                }
            }
        }

        return ApiResult(response)
    } catch (ex: Throwable) {
        ex.printStackTrace()
        return ApiResult(null, ex)
    }
}
