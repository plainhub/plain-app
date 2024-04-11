package com.ismartcoding.lib.readability4j.processor

import com.ismartcoding.lib.readability4j.ArticleMetadata
import org.jsoup.nodes.Document
import java.util.regex.Pattern


internal class MetadataParser {


    fun getArticleMetadata(document: Document): ArticleMetadata {
        val metadata = ArticleMetadata()
        val values = HashMap<String, String>()

        // Match "description", or Twitter's "twitter:description" (Cards)
        // in name attribute.
        val namePattern = Pattern.compile("^\\s*((twitter)\\s*:\\s*)?(description|title)\\s*$", Pattern.CASE_INSENSITIVE)

        // Match Facebook's Open Graph title & description properties.
        val propertyPattern = Pattern.compile("^\\s*og\\s*:\\s*(description|title)\\s*$", Pattern.CASE_INSENSITIVE)

        document.select("meta").forEach { element ->
            val elementName = element.attr("name")
            val elementProperty = element.attr("property")

            if (elementName == "author" || elementProperty == "author") {
                metadata.byline = element.attr("content")
                return@forEach
            }

            var name: String? = null
            if (namePattern.matcher(elementName).find()) {
                name = elementName
            } else if (propertyPattern.matcher(elementProperty).find()) {
                name = elementProperty
            }

            if (name != null) {
                val content = element.attr("content")
                if (!content.isNullOrBlank()) {
                    // Convert to lowercase and remove any whitespace
                    // so we can match below.
                    name = name.lowercase().replace("\\s".toRegex(), "")
                    values[name] = content.trim().replace("  ", " ")
                }
            }
        }

        metadata.excerpt = values["description"] ?: values["og:description"] ?: // Use facebook open graph description.
                values["twitter:description"] // Use twitter cards description.

        metadata.title = getArticleTitle(document)
        if (metadata.title.isBlank()) {
            metadata.title = values["og:title"] ?: // Use facebook open graph title.
                    values["twitter:title"] // Use twitter cards title.
                    ?: ""
        }

        metadata.charset = document.charset().name()

        return metadata
    }

    private fun getArticleTitle(doc: Document): String {
        var curTitle = ""
        var origTitle = ""

        try {
            origTitle = doc.title()
            curTitle = origTitle

            // If they had an element with id "title" in their HTML
            if (curTitle.isBlank()) {
                doc.select("#title").first()?.let { elementWithIdTitle ->
                    origTitle = ProcessorHelper.getInnerText(elementWithIdTitle)
                    curTitle = origTitle
                }
            }
        } catch (e: Exception) {/* ignore exceptions setting the title. */
        }

        var titleHadHierarchicalSeparators = false

        // If there's a separator in the title, first remove the final part
        if (curTitle.contains(" [\\|\\-\\/>»] ".toRegex())) {
            titleHadHierarchicalSeparators = curTitle.contains(" [\\/>»] ".toRegex())
            curTitle = origTitle.replace("(.*)[\\|\\-\\/>»] .*".toRegex(RegexOption.IGNORE_CASE), "$1")

            // If the resulting title is too short (3 words or fewer), remove
            // the first part instead:
            if (wordCount(curTitle) < 3) {
                curTitle = origTitle.replace("[^\\|\\-\\/>»]*[\\|\\-\\/>»](.*)".toRegex(RegexOption.IGNORE_CASE), "$1")
            }
        } else if (curTitle.contains(": ")) {
            // Check if we have an heading containing this exact string, so we
            // could assume it's the full title.
            val match = doc.select("h1, h2").toList().any { it.wholeText() == curTitle }

            // If we don't, let's extract the title out of the original title string.
            if (!match) {
                curTitle = origTitle.substring(origTitle.lastIndexOf(':') + 1)

                // If the title is now too short, try the first colon instead:
                if (wordCount(curTitle) < 3) {
                    curTitle = origTitle.substring(origTitle.indexOf(':') + 1)
                }
                // But if we have too many words before the colon there's something weird
                // with the titles and the H tags so let's just use the original title instead
                else if (wordCount(origTitle.substring(0, origTitle.indexOf(':'))) > 5) {
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
        // If we now have 4 words or fewer as our title, and either no
        // 'hierarchical' separators (\, /, > or ») were found in the original
        // title or we decreased the number of words by more than 1 word, use
        // the original title.
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