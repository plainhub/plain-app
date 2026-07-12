package com.ismartcoding.plain.lib.readability4j

import com.ismartcoding.plain.lib.html2md.HtmlElement


class Article(
    val uri: String
) {

    var title: String = ""

    var articleContent: HtmlElement? = null

    val content: String
        get() = articleContent?.html() ?: ""

    private val contentWithUtf8Encoding: String?
        get() = getContentWithEncoding("utf-8")

    val contentWithDocumentsCharsetOrUtf8: String?
        get() = getContentWithEncoding(charset ?: "utf-8")

    val textContent: String?
        get() = articleContent?.text()

    val length: Int
        get() = textContent?.length ?: -1

    var excerpt: String? = null

    var byline: String? = null

    var dir: String? = null

    var charset: String? = null

    private fun getContentWithEncoding(encoding: String): String {
        return "<html>\n  <head>\n    <meta charset=\"$encoding\"/>\n  </head>\n  <body>\n    " +
                "$content\n  </body>\n</html>"
    }

}
