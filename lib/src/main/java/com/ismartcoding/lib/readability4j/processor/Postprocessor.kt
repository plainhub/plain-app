package com.ismartcoding.lib.readability4j.processor

import com.ismartcoding.lib.logcat.LogCat
import org.jsoup.nodes.Attributes
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Tag
import java.net.URI
import java.util.Arrays
import java.util.regex.Pattern


class Postprocessor {

    companion object {
        val AbsoluteUriPattern: Pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9\\+\\-\\.]*:")


        // These are the classes that readability sets itself.
        val CLASSES_TO_PRESERVE = setOf("readability-styled", "page")
    }


    fun postProcessContent(
        originalDocument: Document, articleContent: Element, articleUri: String
    ) {
        makeLazyLoadingUrlsEagerLoading(articleContent)

        fixAmpImageUris(articleContent)

        // Readability cannot open relative uris so we convert them to absolute uris.
        fixRelativeUris(originalDocument, articleContent, articleUri)

        cleanClasses(articleContent, CLASSES_TO_PRESERVE)
    }


    /**
     * Converts each <a> and <img> uri in the given element to an absolute URI,
     * ignoring #ref URIs.
     */
    private fun fixRelativeUris(originalDocument: Document, element: Element, articleUri: String) {
        try {
            val uri = URI.create(articleUri)
            val scheme = uri.scheme
            val prePath = uri.scheme + "://" + uri.host
            val pathBase = uri.scheme + "://" + uri.host + uri.path.substring(0, uri.path.lastIndexOf("/") + 1)

            fixRelativeUris(originalDocument, element, scheme, prePath, pathBase)
        } catch (e: Exception) {
            LogCat.e("Could not fix relative urls for $element with base uri $articleUri, $e")
        }
    }

    private fun fixRelativeUris(
        originalDocument: Document, element: Element, scheme: String, prePath: String,
        pathBase: String
    ) {
        val baseUrl = originalDocument.head().select("base").first()?.attr("href")
        val url = baseUrl ?: pathBase
        fixRelativeAnchorUris(element, scheme, prePath, url)

        fixRelativeImageUris(element, scheme, prePath, url)
    }

    private fun fixRelativeAnchorUris(element: Element, scheme: String, prePath: String, pathBase: String) {
        element.getElementsByTag("a").forEach { link ->
            val href = link.attr("href")
            if (href.isNotBlank()) {
                // Replace links with javascript: URIs with text content, since
                // they won't work after scripts have been removed from the page.
                if (href.indexOf("javascript:") == 0) {
                    val text = TextNode(link.wholeText())
                    link.replaceWith(text)
                } else {
                    link.attr("href", toAbsoluteURI(href, scheme, prePath, pathBase))
                }
            }
        }
    }

    fun fixRelativeImageUris(element: Element, scheme: String, prePath: String, pathBase: String) {
        element.getElementsByTag("img").forEach { img ->
            fixRelativeImageUri(img, scheme, prePath, pathBase)
        }
    }

    fun fixRelativeImageUri(img: Element, scheme: String, prePath: String, pathBase: String) {
        val src = img.attr("src")

        if (src.isNotBlank()) {
            img.attr("src", toAbsoluteURI(src, scheme, prePath, pathBase))
        }
    }

    fun toAbsoluteURI(uri: String, scheme: String, prePath: String, pathBase: String): String {
        // If this is already an absolute URI, return it.
        if (isAbsoluteUri(uri) || uri.length <= 2) {
            return uri
        }

        // Scheme-rooted relative URI.
        if (uri.substring(0, 2) == "//") {
            return scheme + "://" + uri.substring(2)
        }

        // Prepath-rooted relative URI.
        if (uri[0] == '/') {
            return prePath + uri
        }

        // Dotslash relative URI.
        if (uri.indexOf("./") == 0) {
            return pathBase + uri.substring(2)
        }

        // Ignore hash URIs:
        if (uri[0] == '#') {
            return uri
        }

        // Standard relative URI add entire path. pathBase already includes a
        // trailing "/".
        return pathBase + uri
    }

    private fun isAbsoluteUri(uri: String): Boolean {
        return AbsoluteUriPattern.matcher(uri).find()
    }


    /**
     * Removes the class="" attribute from every element in the given
     * subtree, except those that match CLASSES_TO_PRESERVE and
     * the classesToPreserve array from the options object.
     */
    private fun cleanClasses(node: Element, classesToPreserve: Set<String>) {
        val classNames = node.classNames().filter { classesToPreserve.contains(it) }

        if (classNames.isNotEmpty()) {
            node.classNames(classNames.toMutableSet())
        } else {
            node.removeAttr("class")
        }

        node.children().forEach { child ->
            cleanClasses(child, classesToPreserve)
        }
    }


    private fun makeLazyLoadingUrlsEagerLoading(articleContent: Element) {
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

    private fun makeLazyLoadingUrlEagerLoading(element: Element, attributeToSet: String, lazyLoadingAttributes: List<String>) {
        lazyLoadingAttributes.forEach { lazyLoadingAttributeName ->
            val value = element.attr(lazyLoadingAttributeName)

            if (value.isNotBlank()) { // .attr() by default returns an empty string
                element.attr(attributeToSet, value)

                return // only set first found lazy loading attribute
            }
        }
    }

    private fun fixAmpImageUris(element: Element) {
        element.getElementsByTag("amp-img").forEach { amp_img ->

            if (amp_img.childNodeSize() == 0) {
                val attributes = Attributes()
                attributes.put("decoding", "async")
                attributes.put("alt", amp_img.attr("alt"))
                attributes.put("srcset", amp_img.attr("srcset").trim())

                amp_img.appendChild(Element(Tag.valueOf("img"), "", attributes))
            }
        }
    }
}