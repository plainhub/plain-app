package com.ismartcoding.plain.lib.readability4j.processor

import com.ismartcoding.plain.lib.html2md.HtmlElement
import com.ismartcoding.plain.lib.readability4j.ArticleMetadata

internal class MetadataParser {

    fun getArticleMetadata(document: HtmlElement): ArticleMetadata {
        val metadata = ArticleMetadata()
        val values = HashMap<String, String>()

        val namePattern = Regex("^\\s*((twitter)\\s*:\\s*)?(description|title)\\s*$", RegexOption.IGNORE_CASE)
        val propertyPattern = Regex("^\\s*og\\s*:\\s*(description|title)\\s*$", RegexOption.IGNORE_CASE)

        document.select("meta").forEach { element ->
            val elementName = element.attr("name")
            val elementProperty = element.attr("property")

            if (elementName == "author" || elementProperty == "author") {
                metadata.byline = element.attr("content")
                return@forEach
            }

            var name: String? = null
            if (namePattern.containsMatchIn(elementName)) {
                name = elementName
            } else if (propertyPattern.containsMatchIn(elementProperty)) {
                name = elementProperty
            }

            if (name != null) {
                val content = element.attr("content")
                if (content.isNotBlank()) {
                    name = name.lowercase().replace("\\s".toRegex(), "")
                    values[name] = content.trim().replace("  ", " ")
                }
            }
        }

        metadata.excerpt = values["description"] ?: values["og:description"] ?:
                values["twitter:description"]

        metadata.title = getArticleTitle(document)
        if (metadata.title.isBlank()) {
            metadata.title = values["og:title"] ?:
                    values["twitter:title"]
                    ?: ""
        }

        metadata.charset = document.charset()

        return metadata
    }

    private fun getArticleTitle(doc: HtmlElement): String {
        var curTitle = ""
        var origTitle = ""

        try {
            origTitle = doc.title()
            curTitle = origTitle

            if (curTitle.isBlank()) {
                doc.selectFirst("#title")?.let { elementWithIdTitle ->
                    origTitle = ProcessorHelper.getInnerText(elementWithIdTitle)
                    curTitle = origTitle
                }
            }
        } catch (e: Exception) {/* ignore exceptions setting the title. */
        }

        var titleHadHierarchicalSeparators = false

        if (curTitle.contains(" [\\|\\-\\/>»] ".toRegex())) {
            titleHadHierarchicalSeparators = curTitle.contains(" [\\/>»] ".toRegex())
            curTitle = origTitle.replace("(.*)[\\|\\-\\/>»] .*".toRegex(RegexOption.IGNORE_CASE), "$1")

            if (wordCount(curTitle) < 3) {
                curTitle = origTitle.replace("[^\\|\\-\\/>»]*[\\|\\-\\/>»](.*)".toRegex(RegexOption.IGNORE_CASE), "$1")
            }
        } else if (curTitle.contains(": ")) {
            val match = doc.select("h1, h2").any { it.wholeText() == curTitle }

            if (!match) {
                curTitle = origTitle.substring(origTitle.lastIndexOf(':') + 1)

                if (wordCount(curTitle) < 3) {
                    curTitle = origTitle.substring(origTitle.indexOf(':') + 1)
                } else if (wordCount(origTitle.substring(0, origTitle.indexOf(':'))) > 5) {
                    curTitle = origTitle
                }
            }
        } else if (curTitle.length > 150 || curTitle.length < 15) {
            val hOnes = doc.getElementsByTag("h1")

            if (hOnes.size == 1) {
                curTitle = ProcessorHelper.getInnerText(hOnes[0])
            }
        }

        curTitle = curTitle.trim()
        val curTitleWordCount = wordCount(curTitle)
        if (curTitleWordCount <= 4 &&
            (!titleHadHierarchicalSeparators ||
                    curTitleWordCount != wordCount(origTitle.replace("[\\|\\-\\/>»]+".toRegex(), "")) - 1)
        ) {
            curTitle = origTitle
        }

        return curTitle
    }

    private fun wordCount(str: String): Int {
        return str.split("\\s+".toRegex()).size
    }

}
