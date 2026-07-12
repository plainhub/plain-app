package com.ismartcoding.plain.lib.readability4j


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

    private val unlikelyCandidates = Regex(UnlikelyCandidatesDefaultPattern, RegexOption.IGNORE_CASE)
    private val okMaybeItsACandidate = Regex(OkMaybeItsACandidateDefaultPattern, RegexOption.IGNORE_CASE)
    private val positive = Regex(PositiveDefaultPattern, RegexOption.IGNORE_CASE)
    private val negative = Regex(NegativeDefaultPattern, RegexOption.IGNORE_CASE)
    private val extraneous = Regex(ExtraneousDefaultPattern, RegexOption.IGNORE_CASE)
    private val byline = Regex(BylineDefaultPattern, RegexOption.IGNORE_CASE)
    private val replaceFonts = Regex(ReplaceFontsDefaultPattern, RegexOption.IGNORE_CASE)
    private val normalize = Regex(NormalizeDefaultPattern)
    private val videos = Regex(VideosDefaultPattern, RegexOption.IGNORE_CASE)
    private val nextLink = Regex(NextLinkDefaultPattern, RegexOption.IGNORE_CASE)
    private val prevLink = Regex(PrevLinkDefaultPattern, RegexOption.IGNORE_CASE)
    private val whitespace = Regex(WhitespaceDefaultPattern)
    private val hasContent = Regex(HasContentDefaultPattern)
    private val removeImage = Regex(RemoveImageDefaultPattern, RegexOption.IGNORE_CASE)


    fun isPositive(matchString: String): Boolean {
        return positive.containsMatchIn(matchString)
    }

    fun isNegative(matchString: String): Boolean {
        return negative.containsMatchIn(matchString)
    }

    fun isUnlikelyCandidate(matchString: String): Boolean {
        return unlikelyCandidates.containsMatchIn(matchString)
    }

    fun okMaybeItsACandidate(matchString: String): Boolean {
        return okMaybeItsACandidate.containsMatchIn(matchString)
    }

    fun isByline(matchString: String): Boolean {
        return byline.containsMatchIn(matchString)
    }

    fun hasContent(matchString: String): Boolean {
        return hasContent.containsMatchIn(matchString)
    }

    fun isWhitespace(matchString: String): Boolean {
        return whitespace.containsMatchIn(matchString)
    }

    fun normalize(text: String): String {
        return normalize.replace(text, " ")
    }

    fun isVideo(matchString: String): Boolean {
        return videos.containsMatchIn(matchString)
    }

    fun keepImage(matchString: String): Boolean {
        return !((isNegative(matchString) && !isPositive(matchString)) || removeImage.containsMatchIn(matchString))
    }
}
