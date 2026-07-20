package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import android.os.Environment
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.isOk
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.html2md.MDConverter
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.readability4j.Readability4J
import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.HtmlUtils
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

actual suspend fun DFeedEntry.fetchContentAsync(): ApiResult = withIO {
    try {
        val httpClient = KtorClientFactory.browserClient()
        val response = httpClient.get(url)

        if (response.isOk()) {
            val input = response.body<String>()
            Readability4J.parse(
                url,
                input
            ).articleContent?.let { articleContent ->
                articleContent.selectFirst("h1")?.remove()
                val c = articleContent.toString()
                val mobilizedHtml = HtmlUtils.improveHtmlContent(c, HtmlUtils.getBaseUrl(url))
                val summary = getSummary()
                if (summary.isEmpty() || c.length >= summary.length) { // If the retrieved text is smaller than the original one, then we certainly failed...
                    val imagesList = HtmlUtils.getImageURLs(mobilizedHtml)
                    if (imagesList.isNotEmpty()) {
                        if (image.isEmpty()) {
                            image = HtmlUtils.getMainImageURL(imagesList)
                        }
                    }

                    if (image.isNotEmpty() && !image.startsWith("/")) {
                        try {
                            val r = httpClient.get(image)
                            if (r.isOk()) {
                                val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.path + "/feeds/${feedId}"
                                File(dir).mkdirs()
                                var path = "$dir/main-${sha1(image.toByteArray())}"
                                val extension = image.getFilenameExtension()
                                if (extension.isNotEmpty()) {
                                    path += ".$extension"
                                }
                                val file = File(path)
                                file.createNewFile()
                                r.bodyAsChannel().copyAndClose(file.writeChannel())
                                image = path
                            }
                        } catch (ex: Exception) {
                            LogCat.e(ex.toString())
                            ex.printStackTrace()
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

        return@withIO ApiResult(response)
    } catch (ex: Throwable) {
        ex.printStackTrace()
        return@withIO ApiResult(null, ex)
    }
}
