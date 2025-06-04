package com.ismartcoding.plain.features

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.db.DLinkPreview
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.regex.Pattern

object LinkPreviewHelper {
    private const val MAX_RESPONSE_SIZE = 10 * 1024 * 1024 // 10MB
    private const val MAX_IMAGE_SIZE = 5 * 1024 * 1024 // 5MB
    private const val TIMEOUT_MILLIS = 10000L

    private val URL_PATTERN = Pattern.compile(
        "https?://(?:[-\\w.])+(?:\\:[0-9]+)?(?:/(?:[\\w/_.-]*(?:\\?[\\w&=%.+-]*)?(?:#[\\w.-]*)?)?)?",
        Pattern.CASE_INSENSITIVE
    )

    fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = URL_PATTERN.matcher(text)
        while (matcher.find()) {
            val url = matcher.group()
            if (isValidUrl(url)) {
                urls.add(url)
            }
        }
        return urls.take(5)
    }

    private fun resolveUrl(baseUrl: String, url: String): String {
        return try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else if (url.startsWith("//")) {
                val protocol = URL(baseUrl).protocol
                "$protocol:$url"
            } else if (url.startsWith("/")) {
                val base = URL(baseUrl)
                "${base.protocol}://${base.host}${if (base.port != -1) ":${base.port}" else ""}$url"
            } else {
                val base = URL(baseUrl)
                val basePath = base.path.substringBeforeLast("/")
                "${base.protocol}://${base.host}${if (base.port != -1) ":${base.port}" else ""}$basePath/$url"
            }
        } catch (e: Exception) {
            url
        }
    }

    private fun isValidUrl(url: String): Boolean {
        try {
            val uri = URL(url)
            val host = uri.host.lowercase()

            if (host == "localhost" ||
                host.startsWith("127.") ||
                host.startsWith("192.168.") ||
                host.startsWith("10.") ||
                host.matches(Regex("172\\.(1[6-9]|2[0-9]|3[01])\\..*"))
            ) {
                return false
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun fetchLinkPreview(context: Context, url: String): DLinkPreview {
        return withIO {
            try {
                val client = HttpClientManager.browserClient()
                val response = client.get(url)

                if (!response.status.isSuccess()) {
                    return@withIO DLinkPreview(url = url, hasError = true)
                }

                val contentType = response.headers["Content-Type"]?.lowercase() ?: ""
                if (!contentType.contains("text/html")) {
                    return@withIO DLinkPreview(url = url, hasError = true)
                }

                val contentLength = response.headers["Content-Length"]?.toIntOrNull() ?: 0
                if (contentLength > MAX_RESPONSE_SIZE) {
                    return@withIO DLinkPreview(url = url, hasError = true)
                }

                val htmlContent = response.bodyAsText()
                val domain = URL(url).host

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
                        imageUrl = resolveUrl(url, ogImageMatch.groupValues[1].trim())
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
                                imageUrl = resolveUrl(url, faviconMatch.groupValues[1].trim())
                                break
                            }
                        }

                        if (imageUrl.isNullOrEmpty()) {
                            val baseUrl = URL(url)
                            val defaultFaviconUrl = "${baseUrl.protocol}://${baseUrl.host}/favicon.ico"
                            imageUrl = defaultFaviconUrl
                        }
                    }

                } catch (e: Exception) {
                    LogCat.e("Error parsing HTML: ${e.message}")
                }

                var imageLocalPath: String? = null
                var imageWidth = 0
                var imageHeight = 0
                if (!imageUrl.isNullOrEmpty() && isValidUrl(imageUrl)) {
                    val imageResult = downloadImageWithSize(context, imageUrl, url)
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
                    domain = domain
                )

            } catch (e: Exception) {
                LogCat.e("Error fetching link preview: ${e.message}")
                DLinkPreview(url = url, hasError = true)
            }
        }
    }

    private suspend fun downloadImageWithSize(context: Context, imageUrl: String, originalUrl: String): Triple<String?, Int, Int> {
        return try {
            withContext(Dispatchers.IO) {
                val client = HttpClientManager.browserClient()
                val response = client.get(imageUrl)

                if (!response.status.isSuccess()) {
                    client.close()
                    return@withContext Triple(null, 0, 0)
                }

                val contentType = response.headers["Content-Type"]?.lowercase() ?: ""
                val isFaviconFile = imageUrl.contains("favicon") || imageUrl.endsWith(".ico")
                if (!contentType.startsWith("image/") &&
                    !(isFaviconFile && (contentType.contains("icon") || contentType.contains("octet-stream")))
                ) {
                    client.close()
                    return@withContext Triple(null, 0, 0)
                }

                val imageBytes = response.readRawBytes()
                if (imageBytes.size > MAX_IMAGE_SIZE) {
                    client.close()
                    return@withContext Triple(null, 0, 0)
                }

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                val imageWidth = options.outWidth
                val imageHeight = options.outHeight

                val isFavicon = imageUrl.contains("favicon") || imageUrl.contains("icon") ||
                        (imageWidth < 200 && imageHeight < 200 && imageWidth > 16 && imageHeight > 16)

                if (imageWidth < 100 || imageHeight < 100) {
                    if (!isFavicon) {
                        client.close()
                        return@withContext Triple(null, imageWidth, imageHeight)
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

                val fileName = "preview_${System.currentTimeMillis()}_${URL(originalUrl).host.hashCode().toString().replace("-", "")}.${extension}"
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val previewDir = File(dir, "link_previews")
                if (!previewDir.exists()) {
                    previewDir.mkdirs()
                }

                val file = File(previewDir, fileName)

                if (file.exists()) {
                    client.close()
                    return@withContext Triple("app://${Environment.DIRECTORY_PICTURES}/link_previews/${fileName}", imageWidth, imageHeight)
                }

                file.writeBytes(imageBytes)

                client.close()
                Triple("app://${Environment.DIRECTORY_PICTURES}/link_previews/${fileName}", imageWidth, imageHeight)
            }
        } catch (e: Exception) {
            LogCat.e("Error downloading preview image: ${e.message}")
            Triple(null, 0, 0)
        }
    }

    fun deletePreviewImage(context: Context, imagePath: String) {
        try {
            val file = File(imagePath.getFinalPath(context))
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            LogCat.e("Error deleting preview image: ${e.message}")
        }
    }
} 