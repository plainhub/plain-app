package com.ismartcoding.plain.lib.readability4j.processor

import com.ismartcoding.plain.lib.html2md.HtmlElement
import com.ismartcoding.plain.lib.html2md.HtmlNode
import com.ismartcoding.plain.lib.logcat.LogCat

class Preprocessor {

    fun prepareDocument(document: HtmlElement) {
        LogCat.d("Starting to prepare document")

        removeScripts(document)
        removeNoscripts(document)
        removeStyles(document)
        removeForms(document)
        removeComments(document)
        replaceBrs(document)

        ProcessorHelper.replaceNodes(document, "font", "span")
    }

    private fun removeScripts(document: HtmlElement) {
        ProcessorHelper.removeNodes(document, "script") { scriptNode ->
            scriptNode.`val`("")
            scriptNode.removeAttr("src")
            true
        }
    }

    private fun removeNoscripts(document: HtmlElement) {
        document.getElementsByTag("noscript").forEach { noscript ->
            if (shouldKeepImageInNoscriptElement(document, noscript)) {
                noscript.unwrap()
            } else {
                ProcessorHelper.printAndRemove(noscript, "removeScripts('noscript')")
            }
        }
    }

    private fun shouldKeepImageInNoscriptElement(document: HtmlElement, noscript: HtmlElement): Boolean {
        val images = noscript.select("img")
        if (images.isNotEmpty()) {
            val imagesToKeep = ArrayList(images)

            images.forEach { image ->
                val source = image.attr("src")
                if (source.isNotBlank() && document.select("img[src=$source]").isNotEmpty()) {
                    imagesToKeep.remove(image)
                }
            }

            return imagesToKeep.isNotEmpty()
        }

        return false
    }

    private fun removeStyles(document: HtmlElement) {
        ProcessorHelper.removeNodes(document, "style")
    }

    private fun removeForms(document: HtmlElement) {
        ProcessorHelper.removeNodes(document, "form")
    }

    private fun removeComments(node: HtmlNode) {
        var i = 0
        while (i < node.childNodeSize()) {
            val child = node.childNode(i)
            if (child.nodeName() == "#comment") {
                ProcessorHelper.printAndRemove(child, "removeComments")
            } else {
                removeComments(child)
                i++
            }
        }
    }

    private fun replaceBrs(document: HtmlElement) {
        document.body()?.select("br")?.forEach { br ->
            var next: HtmlNode? = br.nextSibling()

            var replaced = false

            next = ProcessorHelper.nextElement(next)
            while (next != null && next.nodeName() == "br") {
                replaced = true
                val brSibling = (next as? HtmlElement)?.nextSibling()
                ProcessorHelper.printAndRemove(next, "replaceBrs")
                next = ProcessorHelper.nextElement(brSibling)
            }

            if (replaced) {
                val p = br.ownerDocument()?.createElement("p") ?: return@forEach
                br.replaceWith(p)

                next = p.nextSibling()
                while (next != null) {
                    if (next.nodeName() == "br") {
                        val nextElem = ProcessorHelper.nextElement(next)
                        if (nextElem != null && nextElem.tagName() == "br")
                            break
                    }

                    val sibling = next.nextSibling()
                    p.appendChild(next)
                    next = sibling
                }
            }
        }
    }

}
