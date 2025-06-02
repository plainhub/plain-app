package com.ismartcoding.plain.features

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
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
    
    // URL正则表达式，支持http和https
    private val URL_PATTERN = Pattern.compile(
        "https?://(?:[-\\w.])+(?:\\:[0-9]+)?(?:/(?:[\\w/_.-]*(?:\\?[\\w&=%.+-]*)?(?:#[\\w.-]*)?)?)?",
        Pattern.CASE_INSENSITIVE
    )
    
    /**
     * 从文本中提取所有URL
     */
    fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = URL_PATTERN.matcher(text)
        while (matcher.find()) {
            val url = matcher.group()
            if (isValidUrl(url)) {
                urls.add(url)
            }
        }
        return urls.take(5) // 最多5个链接
    }
    
    /**
     * 解析相对URL为绝对URL
     */
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

    /**
     * 验证URL是否有效且安全
     */
    private fun isValidUrl(url: String): Boolean {
        try {
            val uri = URL(url)
            val host = uri.host.lowercase()
            
            // 排除本地和内网地址
            if (host == "localhost" || 
                host.startsWith("127.") ||
                host.startsWith("192.168.") ||
                host.startsWith("10.") ||
                host.matches(Regex("172\\.(1[6-9]|2[0-9]|3[01])\\..*"))) {
                return false
            }
            
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * 异步获取链接预览信息
     */
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
                
                // 解析HTML，提取meta信息
                try {
                    // 提取title
                    val titleMatch = Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE).find(htmlContent)
                    title = titleMatch?.groupValues?.get(1)?.trim()?.take(200)
                    
                    // 提取Open Graph标签
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
                    
                    // 如果没有Open Graph描述，尝试获取meta description
                    if (description.isNullOrEmpty()) {
                        val metaDescMatch = Regex("<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(htmlContent)
                        if (metaDescMatch != null) {
                            description = metaDescMatch.groupValues[1].trim().take(300)
                        }
                    }
                    
                    // 如果没有Open Graph图片，尝试获取favicon或第一个图片
                    if (imageUrl.isNullOrEmpty()) {
                        val faviconMatch = Regex("<link[^>]+rel=[\"'][^\"']*icon[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(htmlContent)
                        if (faviconMatch != null) {
                            imageUrl = resolveUrl(url, faviconMatch.groupValues[1].trim())
                        }
                    }
                    
                } catch (e: Exception) {
                    LogCat.e("Error parsing HTML: ${e.message}")
                }
                
                // 下载并保存图片
                var imageLocalPath: String? = null
                var imageWidth = 0
                var imageHeight = 0
                if (!imageUrl.isNullOrEmpty() && isValidUrl(imageUrl)) {
                    val imageResult = downloadImageWithSize(context, imageUrl, url)
                    imageLocalPath = imageResult.first
                    imageWidth = imageResult.second
                    imageHeight = imageResult.third
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
    
    /**
     * 下载并保存预览图片，返回路径和尺寸信息
     * @return Triple(imagePath, width, height)
     */
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
                if (!contentType.startsWith("image/")) {
                    client.close()
                    return@withContext Triple(null, 0, 0)
                }
                
                val imageBytes = response.readRawBytes()
                if (imageBytes.size > MAX_IMAGE_SIZE) {
                    client.close()
                    return@withContext Triple(null, 0, 0)
                }
                
                // 获取图片尺寸
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                val imageWidth = options.outWidth
                val imageHeight = options.outHeight
                
                // 如果图片太小，不保存
                if (imageWidth < 100 || imageHeight < 100) {
                    client.close()
                    return@withContext Triple(null, imageWidth, imageHeight)
                }
                
                val extension = when {
                    contentType.contains("jpeg") || contentType.contains("jpg") -> "jpg"
                    contentType.contains("png") -> "png"
                    contentType.contains("gif") -> "gif"
                    contentType.contains("webp") -> "webp"
                    else -> "jpg"
                }
                
                val fileName = "preview_${System.currentTimeMillis()}_${URL(originalUrl).host.hashCode().toString().replace("-", "")}.${extension}"
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val previewDir = File(dir, "link_previews")
                if (!previewDir.exists()) {
                    previewDir.mkdirs()
                }
                
                val file = File(previewDir, fileName)
                file.writeBytes(imageBytes)
                
                client.close()
                Triple("app://${Environment.DIRECTORY_PICTURES}/link_previews/${fileName}", imageWidth, imageHeight)
            }
        } catch (e: Exception) {
            LogCat.e("Error downloading preview image: ${e.message}")
            Triple(null, 0, 0)
        }
    }
    
    /**
     * 删除预览图片文件
     */
    fun deletePreviewImage(context: Context, imagePath: String) {
        try {
            if (imagePath.startsWith("app://")) {
                val actualPath = imagePath.substring("app://".length)
                val file = File(context.getExternalFilesDir(null), actualPath)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            LogCat.e("Error deleting preview image: ${e.message}")
        }
    }
} 