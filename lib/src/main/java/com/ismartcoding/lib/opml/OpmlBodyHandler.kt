package com.ismartcoding.lib.opml

import com.ismartcoding.lib.opml.entity.Body
import com.ismartcoding.lib.opml.entity.Outline
import org.xmlpull.v1.XmlPullParser
import java.util.ArrayDeque
import java.util.Deque

internal class OpmlBodyHandler : OpmlSectionHandler<Body> {
    private val stack: Deque<OutlineBuilder> = ArrayDeque()
    private val outlineBuilders: MutableList<OutlineBuilder> = ArrayList()
    private var started = false

    override fun startTag(xpp: XmlPullParser) {
        ValidityCheck.require(xpp, XmlPullParser.START_TAG, "outline")
        val outlineBuilder = parseOutlineBuilder(xpp)
        if (stack.isEmpty()) {
            // this outline is a child of <body>
            outlineBuilders.add(outlineBuilder)
        } else {
            // this outline is nested in a different <outline>
            stack.peek().subElements.add(outlineBuilder)
        }
        stack.push(outlineBuilder)
        started = true
    }

    override fun text(xpp: XmlPullParser) {
        ValidityCheck.requireNoText(xpp, if (stack.isEmpty()) "body" else "outline", started)
    }

    override fun endTag(xpp: XmlPullParser) {
        if (!stack.isEmpty()) {
            stack.pop()
            ValidityCheck.require(xpp, XmlPullParser.END_TAG, "outline")
        } else {
            ValidityCheck.require(xpp, XmlPullParser.END_TAG, "body")
        }
        started = false
    }

    override fun get(): Body {
        val outlines: MutableList<Outline> = ArrayList<Outline>()
        for (subElement in outlineBuilders) {
            outlines.add(build(subElement))
        }
        return Body(outlines)
    }

    private fun build(builder: OutlineBuilder): Outline {
        val subElements: MutableList<Outline> = ArrayList<Outline>()
        for (subElement in builder.subElements) {
            subElements.add(build(subElement))
        }
        return Outline(builder.attributes, subElements)
    }

    private fun parseOutlineBuilder(xpp: XmlPullParser): OutlineBuilder {
        val outlineBuilder = OutlineBuilder()
        for (i in 0 until xpp.attributeCount) {
            val name = xpp.getAttributeName(i)
            if (outlineBuilder.attributes.containsKey(name)) {
                throw OpmlParseException(String.format("element %s contains attribute %s more than once", xpp.name, name))
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
