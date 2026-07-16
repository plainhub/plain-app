package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.lib.html2md.HtmlElement
import com.ismartcoding.plain.lib.html2md.HtmlNode
import com.ismartcoding.plain.lib.html2md.HtmlParser
import com.ismartcoding.plain.lib.html2md.HtmlTextNode

/**
 * Pure-Kotlin HTML utilities for feed content sanitization and image extraction.
 *
 * Replaces the previous jsoup-based implementation so the feed package can
 * live in commonMain without JVM-only dependencies.
 */
object HtmlUtils {
    private const val URL_SPACE = "%20"

    /**
     * Whitelist mirroring jsoup's `Safelist.relaxed()` plus the extra
     * media tags PlainApp accepts (iframe / video / audio / source / track)
     * and a `style` attribute on `<p>`.
     *
     * `img` height/width are intentionally omitted (matches the previous
     * `removeAttributes("img", "height", "width")` call).
     */
    private val WHITELIST: Map<String, Set<String>> = mapOf(
        "a" to setOf("href"),
        "b" to emptySet(),
        "blockquote" to setOf("cite"),
        "br" to emptySet(),
        "caption" to emptySet(),
        "cite" to emptySet(),
        "code" to emptySet(),
        "col" to emptySet(),
        "colgroup" to emptySet(),
        "dd" to emptySet(),
        "div" to emptySet(),
        "dl" to emptySet(),
        "dt" to emptySet(),
        "em" to emptySet(),
        "h1" to emptySet(), "h2" to emptySet(), "h3" to emptySet(),
        "h4" to emptySet(), "h5" to emptySet(), "h6" to emptySet(),
        "hr" to emptySet(),
        "i" to emptySet(),
        "img" to setOf("align", "alt", "src", "title"),
        "li" to emptySet(),
        "ol" to emptySet(),
        "p" to setOf("style"),
        "pre" to emptySet(),
        "q" to setOf("cite"),
        "small" to emptySet(),
        "span" to emptySet(),
        "strike" to emptySet(),
        "strong" to emptySet(),
        "sub" to emptySet(),
        "sup" to emptySet(),
        "table" to emptySet(),
        "tbody" to emptySet(),
        "td" to emptySet(),
        "tfoot" to emptySet(),
        "th" to emptySet(),
        "thead" to emptySet(),
        "tr" to emptySet(),
        "u" to emptySet(),
        "ul" to emptySet(),
        // PlainApp extras (media embeds)
        "iframe" to setOf("src", "frameborder"),
        "video" to setOf("src", "controls", "poster"),
        "audio" to setOf("src", "controls"),
        "source" to setOf("src", "type"),
        "track" to setOf("src", "kind", "srclang", "label"),
    )

    // HTML void elements (no closing tag).
    private val VOID_TAGS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr",
    )

    private val IMG_REGEX = Regex("""<img\s+[^>]*src=\s*['"]([^'"]+)['"][^>]*>""", RegexOption.IGNORE_CASE)
    private val ADS_REGEX = Regex(
        """<div class=('|")mf-viral('|")><table border=('|")0('|")>.*""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val SRCSET_REGEX = Regex("""\s+srcset=\s*['"]([^'"\s]+)[^'"]*['"]""", RegexOption.IGNORE_CASE)
    private val LAZY_LOADING_REGEX = Regex("""\s+src=[^>]+\s+original[-]*src=("|')""", RegexOption.IGNORE_CASE)
    private val PIXEL_IMAGE_REGEX = Regex(
        """<img\s+(height=['"]1['"]\s+width=['"]1['"]|width=['"]1['"]\s+height=['"]1['"])\s+[^>]*src=\s*['"]([^'"]+)['"][^>]*>""",
        RegexOption.IGNORE_CASE,
    )
    private val NON_HTTP_IMAGE_REGEX = Regex("""\s+(href|src)=("|')//""", RegexOption.IGNORE_CASE)
    private val BAD_IMAGE_REGEX = Regex("""<img\s+[^>]*src=\s*['"]([^'"]+)\.img['"][^>]*>""", RegexOption.IGNORE_CASE)
    private val EMPTY_IMAGE_REGEX = Regex("""<img((?!src=).)*?>""", RegexOption.IGNORE_CASE)
    private val START_BR_REGEX = Regex("""^(\s*<br\s*[/]*>\s*)*""", RegexOption.IGNORE_CASE)
    private val END_BR_REGEX = Regex("""(\s*<br\s*[/]*>\s*)*$""", RegexOption.IGNORE_CASE)
    private val MULTIPLE_BR_REGEX = Regex("""(\s*<br\s*[/]*>\s*){3,}""", RegexOption.IGNORE_CASE)
    private val EMPTY_LINK_REGEX = Regex("""<a\s+[^>]*></a>""", RegexOption.IGNORE_CASE)

    fun improveHtmlContent(
        content: String,
        baseUri: String,
    ): String {
        var c = content

        // remove some ads
        c = ADS_REGEX.replace(c, "")
        // take the first image in srcset links
        c = SRCSET_REGEX.replace(c, " src='$1'")
        // remove lazy loading images stuff — original code used `$2` which
        // referred to a non-existent capture group; we simply drop the
        // lazy-loading attribute here.
        c = LAZY_LOADING_REGEX.replace(c, " src=")

        // clean by pure-Kotlin sanitizer
        c = cleanHtml(c, baseUri)

        // remove empty or bad images
        c = PIXEL_IMAGE_REGEX.replace(c, "")
        c = BAD_IMAGE_REGEX.replace(c, "")
        c = EMPTY_IMAGE_REGEX.replace(c, "")
        // remove empty links
        c = EMPTY_LINK_REGEX.replace(c, "")
        // fix non http image paths
        c = NON_HTTP_IMAGE_REGEX.replace(c, " $1=$2http://")
        // remove trailing BR & too much BR
        c = START_BR_REGEX.replace(c, "")
        c = END_BR_REGEX.replace(c, "")
        c = MULTIPLE_BR_REGEX.replace(c, "<br><br>")

        return c
    }

    /**
     * Sanitize HTML using [HtmlParser] and the [WHITELIST].
     *
     * Disallowed tags are unwrapped (their children are kept, preserving
     * inline text content). Disallowed attributes are removed. Relative
     * `src` / `href` URLs are resolved against [baseUri] (matching jsoup's
     * `Jsoup.clean(html, baseUri, safelist)` behavior).
     *
     * Text nodes are escaped on serialization so the output is safe HTML
     * (the shared [HtmlTextNode.outerHtml] deliberately does not escape,
     * because the html2md consumer needs the raw text).
     */
    private fun cleanHtml(html: String, baseUri: String): String {
        val root = HtmlParser.parse(html)
        sanitize(root, baseUri)
        return root.childNodes().joinToString("") { serializeNode(it) }
    }

    private fun sanitize(node: HtmlNode, baseUri: String) {
        // Snapshot the children because unwrap() mutates the parent's child list.
        val children = node.childNodes().toList()
        for (child in children) {
            if (child !is HtmlElement) continue
            val allowedAttrs = WHITELIST[child.tagName()]
            if (allowedAttrs == null) {
                // Tag not in whitelist — recurse first to sanitize descendants,
                // then unwrap (promoting the now-sanitized children to this node).
                sanitize(child, baseUri)
                child.unwrap()
            } else {
                // Remove disallowed attributes and resolve relative URLs.
                val currentAttrs = child.attributes().map { it.key }
                for (attrName in currentAttrs) {
                    if (attrName !in allowedAttrs) {
                        child.removeAttr(attrName)
                    } else if (attrName == "src" || attrName == "href") {
                        val value = child.attr(attrName)
                        val resolved = resolveUrl(baseUri, value)
                        if (resolved != value) {
                            child.attr(attrName, resolved)
                        }
                    }
                }
                sanitize(child, baseUri)
            }
        }
    }

    private fun serializeNode(node: HtmlNode): String = when (node) {
        is HtmlTextNode -> escapeText(node.text())
        is HtmlElement -> serializeElement(node)
        else -> ""
    }

    private fun serializeElement(elem: HtmlElement): String {
        val sb = StringBuilder()
        sb.append('<').append(elem.tagName())
        for (attr in elem.attributes()) {
            sb.append(' ').append(attr.key).append("=\"")
                .append(escapeAttr(attr.value))
                .append('"')
        }
        val children = elem.childNodes()
        if (children.isEmpty() && elem.tagName() in VOID_TAGS) {
            sb.append(" />")
        } else {
            sb.append('>')
            for (c in children) sb.append(serializeNode(c))
            sb.append("</").append(elem.tagName()).append('>')
        }
        return sb.toString()
    }

    private fun escapeText(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun escapeAttr(s: String): String =
        s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")

    private fun resolveUrl(baseUri: String, url: String): String {
        if (url.isEmpty() || baseUri.isEmpty()) return url
        if (url.startsWith("http://") || url.startsWith("https://") ||
            url.startsWith("//") || url.startsWith("mailto:") ||
            url.startsWith("data:") || url.startsWith("#")
        ) {
            return url
        }
        if (url.startsWith("/")) {
            val schemeEnd = baseUri.indexOf("://")
            if (schemeEnd < 0) return url
            val hostEnd = baseUri.indexOf('/', schemeEnd + 3)
            val host = if (hostEnd < 0) baseUri else baseUri.substring(0, hostEnd)
            return host + url
        }
        val lastSlash = baseUri.lastIndexOf('/')
        if (lastSlash < 0) return url
        val baseDir = baseUri.substring(0, lastSlash + 1)
        return baseDir + url
    }

    fun getImageURLs(content: String): ArrayList<String> {
        val images = ArrayList<String>()
        if (content.isNotEmpty()) {
            for (match in IMG_REGEX.findAll(content)) {
                match.groupValues[1].replace(" ", URL_SPACE).let { images.add(it) }
            }
        }
        return images
    }

    fun getMainImageURL(imgUrls: ArrayList<String>): String {
        return imgUrls.firstOrNull { isCorrectImage(it) } ?: ""
    }

    fun getBaseUrl(link: String): String {
        var baseUrl = link
        val index = link.indexOf('/', 8) // this also covers https://
        if (index > -1) {
            baseUrl = link.substring(0, index)
        }
        return baseUrl
    }

    private fun isCorrectImage(imgUrl: String): Boolean {
        if (imgUrl.isEmpty()) {
            return false
        }
        if (!imgUrl.endsWith(".gif", true) && !imgUrl.endsWith(".img", true)) {
            return true
        }
        return false
    }
}
