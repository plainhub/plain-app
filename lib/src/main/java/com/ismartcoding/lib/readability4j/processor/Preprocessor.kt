package com.ismartcoding.lib.readability4j.processor

import com.ismartcoding.lib.logcat.LogCat
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

class Preprocessor {

    /**
     * Prepare the HTML document for readability to scrape it.
     * This includes things like stripping javascript, CSS, and handling terrible markup.
     */
    fun prepareDocument(document: Document) {
        LogCat.d("Starting to prepare document")

        removeScripts(document)
        removeNoscripts(document);

        removeStyles(document)

        removeForms(document)

        removeComments(document)

        replaceBrs(document)

        ProcessorHelper.replaceNodes(document, "font", "span")
    }


    private fun removeScripts(document: Document) {
        ProcessorHelper.removeNodes(document, "script") { scriptNode ->
            scriptNode.`val`("") // TODO: what is this good for?
            scriptNode.removeAttr("src")
            true
        }
    }

    private fun removeNoscripts(document: Document) {
        document.getElementsByTag("noscript").forEach { noscript ->
            if (shouldKeepImageInNoscriptElement(document, noscript)) { // TODO: this is not in Mozilla's Readability
                noscript.unwrap()
            } else {
                ProcessorHelper.printAndRemove(noscript, "removeScripts('noscript')")
            }
        }
    }

    private fun shouldKeepImageInNoscriptElement(document: Document, noscript: Element): Boolean {
        val images = noscript.select("img")
        if (images.size > 0) {
            val imagesToKeep = ArrayList(images)

            images.forEach { image ->
                // thanks to swuqi (https://github.com/swuqi) for reporting this bug.
                // see https://github.com/dankito/Readability4J/issues/4
                val source = image.attr("src")
                if (source.isNotBlank() && document.select("img[src=$source]").size > 0) {
                    imagesToKeep.remove(image)
                }
            }

            return imagesToKeep.size > 0
        }

        return false
    }

    private fun removeStyles(document: Document) {
        ProcessorHelper.removeNodes(document, "style")
    }

    private fun removeForms(document: Document) {
        ProcessorHelper.removeNodes(document, "form")
    }

    private fun removeComments(node: Node) {
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


    /**
     * Replaces 2 or more successive <br> elements with a single <p>.
     * Whitespace between <br> elements are ignored. For example:
     *   <div>foo<br>bar<br> <br><br>abc</div>
     * will become:
     *   <div>foo<br>bar<p>abc</p></div>
     */
    private fun replaceBrs(document: Document) {
        document.body().select("br").forEach { br ->
            var next: Node? = br.nextSibling()

            // Whether 2 or more <br> elements have been found and replaced with a
            // <p> block.
            var replaced = false

            // If we find a <br> chain, remove the <br>s until we hit another element
            // or non-whitespace. This leaves behind the first <br> in the chain
            // (which will be replaced with a <p> later).
            next = ProcessorHelper.nextElement(next)
            while (next != null && next.nodeName() == "br") {
                replaced = true
                val brSibling = (next as? Element)?.nextSibling()
                ProcessorHelper.printAndRemove(next, "replaceBrs")
                next = ProcessorHelper.nextElement(brSibling)
            }

            // If we removed a <br> chain, replace the remaining <br> with a <p>. Add
            // all sibling nodes as children of the <p> until we hit another <br>
            // chain.
            if (replaced) {
                val p = br.ownerDocument()?.createElement("p") ?: return@forEach
                br.replaceWith(p)

                next = p.nextSibling()
                while (next != null) {
                    // If we've hit another <br><br>, we're done adding children to this <p>.
                    if (next.nodeName() == "br") {
                        val nextElem = ProcessorHelper.nextElement(next)
                        if (nextElem != null && nextElem.tagName() == "br")
                            break
                    }

                    // Otherwise, make this node a child of the new <p>.
                    val sibling = next.nextSibling()
                    p.appendChild(next)
                    next = sibling
                }
            }
        }
    }

}
