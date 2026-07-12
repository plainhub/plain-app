package com.ismartcoding.plain.lib.opml

import com.ismartcoding.plain.lib.opml.entity.Body
import com.ismartcoding.plain.lib.opml.entity.Outline

internal class OpmlBodyHandler : OpmlSectionHandler<Body> {
    private val stack = ArrayDeque<OutlineBuilder>()
    private val outlineBuilders: MutableList<OutlineBuilder> = ArrayList()
    private var started = false

    override fun startTag(xpp: SimpleXmlReader) {
        ValidityCheck.require(xpp, SimpleXmlReader.START_TAG, "outline")
        val outlineBuilder = parseOutlineBuilder(xpp)
        if (stack.isEmpty()) {
            outlineBuilders.add(outlineBuilder)
        } else {
            stack.last().subElements.add(outlineBuilder)
        }
        stack.addLast(outlineBuilder)
        started = true
    }

    override fun text(xpp: SimpleXmlReader) {
        ValidityCheck.requireNoText(xpp, if (stack.isEmpty()) "body" else "outline", started)
    }

    override fun endTag(xpp: SimpleXmlReader) {
        if (stack.isNotEmpty()) {
            stack.removeLast()
            ValidityCheck.require(xpp, SimpleXmlReader.END_TAG, "outline")
        } else {
            ValidityCheck.require(xpp, SimpleXmlReader.END_TAG, "body")
        }
        started = false
    }

    override fun get(): Body {
        val outlines: MutableList<Outline> = ArrayList()
        for (subElement in outlineBuilders) {
            outlines.add(build(subElement))
        }
        return Body(outlines)
    }

    private fun build(builder: OutlineBuilder): Outline {
        val subElements: MutableList<Outline> = ArrayList()
        for (subElement in builder.subElements) {
            subElements.add(build(subElement))
        }
        return Outline(builder.attributes, subElements)
    }

    private fun parseOutlineBuilder(xpp: SimpleXmlReader): OutlineBuilder {
        val outlineBuilder = OutlineBuilder()
        for (i in 0 until xpp.attributeCount) {
            val attrName = xpp.getAttributeName(i)
            if (outlineBuilder.attributes.containsKey(attrName)) {
                throw OpmlParseException("element ${xpp.name} contains attribute $attrName more than once")
            }
            outlineBuilder.attributes[xpp.getAttributeName(i)] = xpp.getAttributeValue(i)
        }
        return outlineBuilder
    }

    private class OutlineBuilder {
        val attributes: MutableMap<String, String> = HashMap()
        val subElements: MutableList<OutlineBuilder> = ArrayList()
    }
}
