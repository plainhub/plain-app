package com.ismartcoding.plain.features

import com.ismartcoding.plain.db.DLinkPreview

object LinkPreviewHelper {
    private const val MAX_RESPONSE_SIZE = 10 * 1024 * 1024 // 10MB
    private const val MAX_IMAGE_SIZE = 5 * 1024 * 1024 // 5MB
    private const val TIMEOUT_MILLIS = 10000L

    private val URL_REGEX = Regex(
        "https?://(?:[-\\w.])+(?:\\:[0-9]+)?(?:/(?:[\\w/_.-]*(?:\\?[\\w&=%.+-]*)?(?:#[\\w.-]*)?)?)?",
        RegexOption.IGNORE_CASE
    )

    fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        for (match in URL_REGEX.findAll(text)) {
            val url = match.value
            if (isValidUrl(url)) {
                urls.add(url)
            }
        }
        return urls.take(5)
    }

    fun resolveUrl(baseUrl: String, url: String): String {
        return try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else if (url.startsWith("//")) {
                val protocol = baseUrl.substringBefore("://", "https")
                "$protocol:$url"
            } else if (url.startsWith("/")) {
                val protocol = baseUrl.substringBefore("://", "https")
                val rest = baseUrl.substringAfter("://")
                val host = rest.substringBefore("/")
                val portPart = if (host.contains(":")) ":${host.substringAfter(":")}" else ""
                val hostOnly = host.substringBefore(":")
                "$protocol://$hostOnly$portPart$url"
            } else {
                val protocol = baseUrl.substringBefore("://", "https")
                val rest = baseUrl.substringAfter("://")
                val host = rest.substringBefore("/")
                val portPart = if (host.contains(":")) ":${host.substringAfter(":")}" else ""
                val hostOnly = host.substringBefore(":")
                val basePath = rest.substringBeforeLast("/").substringAfter(host).ifEmpty { "" }
                "$protocol://$hostOnly$portPart$basePath/$url"
            }
        } catch (_: Exception) {
            url
        }
    }

    fun isValidUrl(url: String): Boolean {
        return try {
            // Parse host from URL: protocol://host[:port]/path
            val withoutProtocol = url.substringAfter("://", "")
            if (withoutProtocol.isEmpty()) return false
            val host = withoutProtocol.substringBefore("/").substringBefore(":").lowercase()
            if (host.isEmpty()) return false

            if (host == "localhost" ||
                host.startsWith("127.") ||
                host.startsWith("192.168.") ||
                host.startsWith("10.") ||
                host.matches(Regex("172\\.(1[6-9]|2[0-9]|3[01])\\..*"))
            ) {
                return false
            }

            true
        } catch (_: Exception) {
            false
        }
    }
}
