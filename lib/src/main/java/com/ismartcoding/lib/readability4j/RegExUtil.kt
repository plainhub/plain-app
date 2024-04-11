package com.ismartcoding.lib.readability4j

import java.util.regex.Pattern


internal object RegExUtil {
    private const val UnlikelyCandidatesDefaultPattern = "banner|breadcrumbs|combx|comment|community|cover-wrap|disqus|extra|" +
            "foot|header|legends|menu|related|remark|replies|rss|shoutbox|sidebar|skyscraper|social|sponsor|supplemental|" +
            "ad-break|agegate|pagination|pager|popup|yom-remote"

    private const val OkMaybeItsACandidateDefaultPattern = "and|article|body|column|main|shadow"

    private const val PositiveDefaultPattern = "article|body|content|entry|hentry|h-entry|main|page|pagination|post|text|blog|story"

    private const val NegativeDefaultPattern = "hidden|^hid$| hid$| hid |^hid |banner|combx|comment|com-|contact|foot|footer|footnote|" +
            "masthead|media|meta|outbrain|promo|related|scroll|share|shoutbox|sidebar|skyscraper|sponsor|shopping|tags|tool|widget"

    private const val ExtraneousDefaultPattern = "print|archive|comment|discuss|e[\\-]?mail|share|reply|all|login|sign|single|utility"

    private const val BylineDefaultPattern = "byline|author|dateline|writtenby|p-author"

    private const val ReplaceFontsDefaultPattern = "<(/?)font[^>]*>"

    private const val NormalizeDefaultPattern = "\\s{2,}"

    private  const val VideosDefaultPattern = "//(www\\.)?(dailymotion|youtube|youtube-nocookie|player\\.vimeo)\\.com"

    private  const val NextLinkDefaultPattern = "(next|weiter|continue|>([^\\|]|$)|»([^\\|]|$))"

    private const val PrevLinkDefaultPattern = "(prev|earl|old|new|<|«)"

    private const val WhitespaceDefaultPattern = "^\\s*$"

    private const val HasContentDefaultPattern = "\\S$"

    private const val RemoveImageDefaultPattern = "author|avatar|thumbnail" // CHANGE: this is not in Mozilla's Readability

    private const val NegativeDefaultPatternExtended = "|float"

    private val unlikelyCandidates = Pattern.compile(UnlikelyCandidatesDefaultPattern, Pattern.CASE_INSENSITIVE)
    private val okMaybeItsACandidate = Pattern.compile(OkMaybeItsACandidateDefaultPattern, Pattern.CASE_INSENSITIVE)
    private val positive = Pattern.compile(PositiveDefaultPattern, Pattern.CASE_INSENSITIVE)
    private val negative = Pattern.compile(NegativeDefaultPattern, Pattern.CASE_INSENSITIVE)
    private val extraneous = Pattern.compile(ExtraneousDefaultPattern, Pattern.CASE_INSENSITIVE)
    private val byline = Pattern.compile(BylineDefaultPattern, Pattern.CASE_INSENSITIVE)
    private val replaceFonts = Pattern.compile(ReplaceFontsDefaultPattern, Pattern.CASE_INSENSITIVE)
    private val normalize = Pattern.compile(NormalizeDefaultPattern)
    private val videos = Pattern.compile(VideosDefaultPattern, Pattern.CASE_INSENSITIVE)
    private val nextLink = Pattern.compile(NextLinkDefaultPattern, Pattern.CASE_INSENSITIVE)
    private val prevLink = Pattern.compile(PrevLinkDefaultPattern, Pattern.CASE_INSENSITIVE)
    private val whitespace = Pattern.compile(WhitespaceDefaultPattern)
    private val hasContent = Pattern.compile(HasContentDefaultPattern)
    private val removeImage = Pattern.compile(RemoveImageDefaultPattern, Pattern.CASE_INSENSITIVE)


    fun isPositive(matchString: String): Boolean {
        return positive.matcher(matchString).find()
    }

    fun isNegative(matchString: String): Boolean {
        return negative.matcher(matchString).find()
    }

    fun isUnlikelyCandidate(matchString: String): Boolean {
        return unlikelyCandidates.matcher(matchString).find()
    }

    fun okMaybeItsACandidate(matchString: String): Boolean {
        return okMaybeItsACandidate.matcher(matchString).find()
    }

    fun isByline(matchString: String): Boolean {
        return byline.matcher(matchString).find()
    }

    fun hasContent(matchString: String): Boolean {
        return hasContent.matcher(matchString).find()
    }

    fun isWhitespace(matchString: String): Boolean {
        return whitespace.matcher(matchString).find()
    }

    fun normalize(text: String): String {
        return normalize.matcher(text).replaceAll(" ")
    }

    fun isVideo(matchString: String): Boolean {
        return videos.matcher(matchString).find()
    }

    fun keepImage(matchString: String): Boolean {
        return !((isNegative(matchString) && !isPositive(matchString)) || removeImage.matcher(matchString).find())
    }
}