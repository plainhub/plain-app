package com.ismartcoding.lib

import net.dankito.readability4j.processor.Preprocessor
import net.dankito.readability4j.util.RegExUtil
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Readability4JPreprocessor(regEx: RegExUtil = RegExUtil()) : Preprocessor(regEx) {
    protected override fun shouldKeepImageInNoscriptElement(
        document: Document,
        noscript: Element,
    ): Boolean {
        val images = noscript.select("img")
        if (images.size > 0) {
            val imagesToKeep = ArrayList(images)

            images.forEach { image ->
                // thanks to swuqi (https://github.com/swuqi) for reporting this bug.
                // see https://github.com/dankito/Readability4J/issues/4
                val source = image.attr("src")
                if (source.isNotBlank() && document.select("img[src=\"$source\"]").size > 0) {
                    imagesToKeep.remove(image)
                }
            }

            return imagesToKeep.size > 0
        }

        return false
    }
}
