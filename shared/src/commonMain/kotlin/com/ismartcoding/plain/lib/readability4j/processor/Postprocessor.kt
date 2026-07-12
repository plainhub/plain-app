package com.ismartcoding.plain.lib.readability4j.processor

import com.ismartcoding.plain.lib.html2md.HtmlElement
import com.ismartcoding.plain.lib.html2md.HtmlTextNode
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlin.math.floor

class Postprocessor {

    companion object {
        val AbsoluteUriPattern = Regex("^[a-zA-Z][a-zA-Z0-9\\+\\-\\.]*:")

        val CLASSES_TO_PRESERVE = setOf("readability-styled", "page")
    }

    fun postProcessContent(
        originalDocument: HtmlElement, articleContent: HtmlElement, articleUri: String
    ) {
        makeLazyLoadingUrlsEagerLoading(articleContent)
        fixAmpImageUris(articleContent)
        fixRelativeUris(originalDocument, articleContent, articleUri)
        cleanClasses(articleContent, CLASSES_TO_PRESERVE)
    }

    private fun fixRelativeUris(originalDocument: HtmlElement, element: HtmlElement, articleUri: String) {
        try {
            val uri = parseUri(articleUri)
            val scheme = uri.scheme
            val prePath = uri.scheme + "://" + uri.host
            val pathBase = uri.scheme + "://" + uri.host + uri.path.substring(0, uri.path.lastIndexOf("/") + 1)

            fixRelativeUris(originalDocument, element, scheme, prePath, pathBase)
        } catch (e: Exception) {
            LogCat.e("Could not fix relative urls for $element with base uri $articleUri, $e")
        }
    }

    private data class SimpleUri(val scheme: String, val host: String, val path: String)

    private fun parseUri(uri: String): SimpleUri {
        val match = Regex("^(\\w+)://([^/]+)(/.*)?$").find(uri)
        return if (match != null) {
            SimpleUri(match.groupValues[1], match.groupValues[2], match.groupValues[3].ifEmpty { "/" })
        } else {
            SimpleUri("", "", "/")
        }
    }

    private fun fixRelativeUris(
        originalDocument: HtmlElement, element: HtmlElement, scheme: String, prePath: String,
        pathBase: String
    ) {
        val baseUrl = originalDocument.head()?.selectFirst("base")?.attr("href")
        val url = baseUrl ?: pathBase
        fixRelativeAnchorUris(element, scheme, prePath, url)
        fixRelativeImageUris(element, scheme, prePath, url)
    }

    private fun fixRelativeAnchorUris(element: HtmlElement, scheme: String, prePath: String, pathBase: String) {
        element.getElementsByTag("a").forEach { link ->
            val href = link.attr("href")
            if (href.isNotBlank()) {
                if (href.startsWith("javascript:")) {
                    val text = HtmlTextNode(link.wholeText())
                    link.replaceWith(text)
                } else {
                    link.attr("href", toAbsoluteURI(href, scheme, prePath, pathBase))
                }
            }
        }
    }

    fun fixRelativeImageUris(element: HtmlElement, scheme: String, prePath: String, pathBase: String) {
        element.getElementsByTag("img").forEach { img ->
            fixRelativeImageUri(img, scheme, prePath, pathBase)
        }
    }

    fun fixRelativeImageUri(img: HtmlElement, scheme: String, prePath: String, pathBase: String) {
        val src = img.attr("src")
        if (src.isNotBlank()) {
            img.attr("src", toAbsoluteURI(src, scheme, prePath, pathBase))
        }
    }

    fun toAbsoluteURI(uri: String, scheme: String, prePath: String, pathBase: String): String {
        if (isAbsoluteUri(uri) || uri.length <= 2) {
            return uri
        }

        if (uri.startsWith("//")) {
            return scheme + "://" + uri.substring(2)
        }

        if (uri[0] == '/') {
            return prePath + uri
        }

        if (uri.startsWith("./")) {
            return pathBase + uri.substring(2)
        }

        if (uri[0] == '#') {
            return uri
        }

        return pathBase + uri
    }

    private fun isAbsoluteUri(uri: String): Boolean {
        return AbsoluteUriPattern.containsMatchIn(uri)
    }

    private fun cleanClasses(node: HtmlElement, classesToPreserve: Set<String>) {
        val classNames = node.classNames().filter { classesToPreserve.contains(it) }

        if (classNames.isNotEmpty()) {
            node.classNames(classNames.toSet())
        } else {
            node.removeAttr("class")
        }

        node.children().forEach { child ->
            cleanClasses(child, classesToPreserve)
        }
    }

    private fun makeLazyLoadingUrlsEagerLoading(articleContent: HtmlElement) {
        articleContent.select("img").forEach { imgElement ->
            makeLazyLoadingUrlEagerLoading(
                imgElement, "src",
                listOf(
                    "data-src", "data-original", "data-actualsrc", "data-lazy-src", "data-delayed-url",
                    "data-li-src", "data-pagespeed-lazy-src"
                )
            )
        }
    }

    private fun makeLazyLoadingUrlEagerLoading(element: HtmlElement, attributeToSet: String, lazyLoadingAttributes: List<String>) {
        lazyLoadingAttributes.forEach { lazyLoadingAttributeName ->
            val value = element.attr(lazyLoadingAttributeName)
            if (value.isNotBlank()) {
                element.attr(attributeToSet, value)
                return
            }
        }
    }

    private fun fixAmpImageUris(element: HtmlElement) {
        element.getElementsByTag("amp-img").forEach { ampImg ->
            if (ampImg.childNodeSize() == 0) {
                val img = HtmlElement("img")
                img.attr("decoding", "async")
                img.attr("alt", ampImg.attr("alt"))
                img.attr("srcset", ampImg.attr("srcset").trim())
                ampImg.appendChild(img)
            }
        }
    }
}
