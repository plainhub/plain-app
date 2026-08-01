@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DLinkPreview
import com.ismartcoding.plain.features.LinkPreviewHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.toNSData
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.writeToFile
import platform.UIKit.UIImage

private const val MAX_RESPONSE_SIZE = 10 * 1024 * 1024
private const val MAX_IMAGE_SIZE = 5 * 1024 * 1024

actual suspend fun fetchLinkPreviewsAsync(urls: List<String>): List<DLinkPreview> {
    if (urls.isEmpty()) return emptyList()
    return try {
        coroutineScope {
            urls.map { url -> async { fetchLinkPreview(url) } }.awaitAll()
        }
    } catch (e: Exception) {
        LogCat.e("fetchLinkPreviewsAsync: ${e.message}")
        emptyList()
    }
}

actual fun deletePreviewImage(imagePath: String) {
    try {
        val resolved = if (imagePath.startsWith("app://")) {
            appDir() + "/" + imagePath.removePrefix("app://")
        } else {
            imagePath
        }
        NSFileManager.defaultManager.removeItemAtPath(resolved, null)
    } catch (e: Exception) {
        LogCat.e("deletePreviewImage: ${e.message}")
    }
}

private fun extractHost(url: String): String {
    val nsUrl = NSURL.URLWithString(url) ?: return ""
    return nsUrl.host ?: ""
}

private suspend fun fetchLinkPreview(url: String): DLinkPreview {
    return try {
        val client = KtorClientFactory.browserClient()
        val response = client.get(url)

        if (!response.status.isSuccess()) {
            client.close()
            return DLinkPreview(url = url, hasError = true)
        }

        val contentType = response.headers["Content-Type"]?.lowercase() ?: ""
        if (!contentType.contains("text/html")) {
            client.close()
            return DLinkPreview(url = url, hasError = true)
        }

        val contentLength = response.headers["Content-Length"]?.toIntOrNull() ?: 0
        if (contentLength > MAX_RESPONSE_SIZE) {
            client.close()
            return DLinkPreview(url = url, hasError = true)
        }

        val htmlContent = response.bodyAsText()
        val domain = extractHost(url)

        var title: String? = null
        var description: String? = null
        var imageUrl: String? = null
        var siteName: String? = null

        try {
            val titleMatch = Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE).find(htmlContent)
            title = titleMatch?.groupValues?.get(1)?.trim()?.take(200)

            val ogTitleMatch = Regex("<meta[^>]+property=[\"']og:title[\"'][^>]+content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(htmlContent)
            if (ogTitleMatch != null) {
                title = ogTitleMatch.groupValues[1].trim().take(200)
            }

            val ogDescMatch = Regex("<meta[^>]+property=[\"']og:description[\"'][^>]+content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(htmlContent)
            if (ogDescMatch != null) {
                description = ogDescMatch.groupValues[1].trim().take(300)
            }

            val ogImageMatch = Regex("<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(htmlContent)
            if (ogImageMatch != null) {
                imageUrl = LinkPreviewHelper.resolveUrl(url, ogImageMatch.groupValues[1].trim())
            }

            val ogSiteMatch = Regex("<meta[^>]+property=[\"']og:site_name[\"'][^>]+content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(htmlContent)
            if (ogSiteMatch != null) {
                siteName = ogSiteMatch.groupValues[1].trim().take(100)
            }

            if (description.isNullOrEmpty()) {
                val metaDescMatch = Regex("<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(htmlContent)
                if (metaDescMatch != null) {
                    description = metaDescMatch.groupValues[1].trim().take(300)
                }
            }

            if (imageUrl.isNullOrEmpty()) {
                val faviconPatterns = listOf(
                    "<link[^>]+rel=[\"'][^\"']*icon[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']",
                    "<link[^>]+href=[\"']([^\"']+)[\"'][^>]+rel=[\"'][^\"']*icon[^\"']*[\"']",
                    "<link[^>]+rel=[\"']shortcut icon[\"'][^>]+href=[\"']([^\"']+)[\"']",
                    "<link[^>]+rel=[\"']apple-touch-icon[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']"
                )
                for (pattern in faviconPatterns) {
                    val faviconMatch = Regex(pattern, RegexOption.IGNORE_CASE).find(htmlContent)
                    if (faviconMatch != null) {
                        imageUrl = LinkPreviewHelper.resolveUrl(url, faviconMatch.groupValues[1].trim())
                        break
                    }
                }
                if (imageUrl.isNullOrEmpty()) {
                    val nsUrl = NSURL.URLWithString(url)
                    if (nsUrl != null) {
                        val scheme = nsUrl.scheme ?: "https"
                        val host = nsUrl.host ?: ""
                        if (host.isNotEmpty()) {
                            imageUrl = "$scheme://$host/favicon.ico"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LogCat.e("fetchLinkPreview parse: ${e.message}")
        }

        var imageLocalPath: String? = null
        var imageWidth = 0
        var imageHeight = 0
        if (!imageUrl.isNullOrEmpty() && LinkPreviewHelper.isValidUrl(imageUrl)) {
            val imageResult = downloadImageWithSize(imageUrl, url)
            imageLocalPath = imageResult.first
            imageWidth = imageResult.second
            imageHeight = imageResult.third
            if (imageLocalPath == null && imageUrl.endsWith("/favicon.ico")) {
                imageUrl = null
            }
        }

        client.close()
        DLinkPreview(
            url = url,
            title = title?.ifEmpty { null },
            description = description?.ifEmpty { null },
            imageUrl = imageUrl?.ifEmpty { null },
            imageLocalPath = imageLocalPath,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            siteName = siteName?.ifEmpty { null },
            domain = domain,
        )
    } catch (e: Exception) {
        LogCat.e("fetchLinkPreview: ${e.message}")
        DLinkPreview(url = url, hasError = true)
    }
}

private suspend fun downloadImageWithSize(imageUrl: String, originalUrl: String): Triple<String?, Int, Int> {
    return try {
        val client = KtorClientFactory.browserClient()
        val response = client.get(imageUrl)

        if (!response.status.isSuccess()) {
            client.close()
            return Triple(null, 0, 0)
        }

        val contentType = response.headers["Content-Type"]?.lowercase() ?: ""
        val isFaviconFile = imageUrl.contains("favicon") || imageUrl.endsWith(".ico")
        if (!contentType.startsWith("image/") &&
            !(isFaviconFile && (contentType.contains("icon") || contentType.contains("octet-stream")))
        ) {
            client.close()
            return Triple(null, 0, 0)
        }

        val imageBytes = response.body<ByteArray>()
        if (imageBytes.size > MAX_IMAGE_SIZE) {
            client.close()
            return Triple(null, 0, 0)
        }

        val nsData = imageBytes.toNSData()

        val uiImage = UIImage.imageWithData(nsData)
        val imageWidth = uiImage?.size?.useContents { width.toInt() } ?: 0
        val imageHeight = uiImage?.size?.useContents { height.toInt() } ?: 0

        val isFavicon = imageUrl.contains("favicon") || imageUrl.contains("icon") ||
            (imageWidth < 200 && imageHeight < 200 && imageWidth > 16 && imageHeight > 16)

        if (imageWidth < 100 || imageHeight < 100) {
            if (!isFavicon) {
                client.close()
                return Triple(null, imageWidth, imageHeight)
            }
        }

        val extension = when {
            contentType.contains("jpeg") || contentType.contains("jpg") -> "jpg"
            contentType.contains("png") -> "png"
            contentType.contains("gif") -> "gif"
            contentType.contains("webp") -> "webp"
            contentType.contains("ico") -> "ico"
            else -> imageUrl.getFilenameExtension().ifEmpty { "jpg" }
        }

        val host = extractHost(originalUrl)
        val fileName = "preview_${TimeHelper.now().toEpochMilliseconds()}_${host.hashCode().toString().replace("-", "")}.$extension"
        val previewDir = appDir() + "/link_previews"
        NSFileManager.defaultManager.createDirectoryAtPath(
            previewDir, withIntermediateDirectories = true, attributes = null, error = null,
        )
        val filePath = "$previewDir/$fileName"
        nsData.writeToFile(filePath, atomically = true)

        client.close()
        Triple("app://link_previews/$fileName", imageWidth, imageHeight)
    } catch (e: Exception) {
        LogCat.e("downloadImageWithSize: ${e.message}")
        Triple(null, 0, 0)
    }
}
