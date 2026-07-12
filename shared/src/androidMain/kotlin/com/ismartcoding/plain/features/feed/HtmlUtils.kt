package com.ismartcoding.plain.features.feed

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.util.regex.Pattern

object HtmlUtils {
    private val JSOUP_WHITELIST =
        Safelist.relaxed().addTags("iframe", "video", "audio", "source", "track")
            .addAttributes("iframe", "src", "frameborder")
            .addAttributes("video", "src", "controls", "poster")
            .addAttributes("audio", "src", "controls")
            .addAttributes("source", "src", "type")
            .addAttributes("track", "src", "kind", "srclang", "label")
            .addAttributes("p", "style")
            .removeAttributes("img", "height", "width")

    private const val URL_SPACE = "%20"

    private val IMG_PATTERN = Pattern.compile("<img\\s+[^>]*src=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE)
    private val ADS_PATTERN = Pattern.compile("<div class=('|\")mf-viral('|\")><table border=('|\")0('|\")>.*", Pattern.CASE_INSENSITIVE)
    private val SRCSET_PATTERN = Pattern.compile("\\s+srcset=\\s*['\"]([^'\"\\s]+)[^'\"]*['\"]", Pattern.CASE_INSENSITIVE)
    private val LAZY_LOADING_PATTERN = Pattern.compile("\\s+src=[^>]+\\s+original[-]*src=(\"|')", Pattern.CASE_INSENSITIVE)
    private val PIXEL_IMAGE_PATTERN =
        Pattern.compile(
            "<img\\s+(height=['\"]1['\"]\\s+width=['\"]1['\"]|width=['\"]1['\"]\\s+height=['\"]1['\"])\\s+[^>]*src=\\s*['\"]([^'\"]+)['\"][^>]*>",
            Pattern.CASE_INSENSITIVE,
        )
    private val NON_HTTP_IMAGE_PATTERN = Pattern.compile("\\s+(href|src)=(\"|')//", Pattern.CASE_INSENSITIVE)
    private val BAD_IMAGE_PATTERN = Pattern.compile("<img\\s+[^>]*src=\\s*['\"]([^'\"]+)\\.img['\"][^>]*>", Pattern.CASE_INSENSITIVE)
    private val EMPTY_IMAGE_PATTERN = Pattern.compile("<img((?!src=).)*?>", Pattern.CASE_INSENSITIVE)
    private val START_BR_PATTERN = Pattern.compile("^(\\s*<br\\s*[/]*>\\s*)*", Pattern.CASE_INSENSITIVE)
    private val END_BR_PATTERN = Pattern.compile("(\\s*<br\\s*[/]*>\\s*)*$", Pattern.CASE_INSENSITIVE)
    private val MULTIPLE_BR_PATTERN = Pattern.compile("(\\s*<br\\s*[/]*>\\s*){3,}", Pattern.CASE_INSENSITIVE)
    private val EMPTY_LINK_PATTERN = Pattern.compile("<a\\s+[^>]*></a>", Pattern.CASE_INSENSITIVE)

    fun improveHtmlContent(
        content: String,
        baseUri: String,
    ): String {
        var c = content

        // remove some ads
        c = ADS_PATTERN.matcher(c).replaceAll("")
        // take the first image in srcset links
        c = SRCSET_PATTERN.matcher(c).replaceAll(" src='$1'")
        // remove lazy loading images stuff
        c = LAZY_LOADING_PATTERN.matcher(c).replaceAll(" src=$2")

        // clean by JSoup
        c = Jsoup.clean(c, baseUri, JSOUP_WHITELIST)

        // remove empty or bad images
        c = PIXEL_IMAGE_PATTERN.matcher(c).replaceAll("")
        c = BAD_IMAGE_PATTERN.matcher(c).replaceAll("")
        c = EMPTY_IMAGE_PATTERN.matcher(c).replaceAll("")
        // remove empty links
        c = EMPTY_LINK_PATTERN.matcher(c).replaceAll("")
        // fix non http image paths
        c = NON_HTTP_IMAGE_PATTERN.matcher(c).replaceAll(" $1=$2http://")
        // remove trailing BR & too much BR
        c = START_BR_PATTERN.matcher(c).replaceAll("")
        c = END_BR_PATTERN.matcher(c).replaceAll("")
        c = MULTIPLE_BR_PATTERN.matcher(c).replaceAll("<br><br>")

        return c
    }

    fun getImageURLs(content: String): ArrayList<String> {
        val images = ArrayList<String>()

        if (content.isNotEmpty()) {
            val matcher = IMG_PATTERN.matcher(content)

            while (matcher.find()) {
                matcher.group(1)?.replace(" ", URL_SPACE)?.let { images.add(it) }
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
