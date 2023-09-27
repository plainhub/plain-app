package com.ismartcoding.lib.opml

import com.ismartcoding.lib.opml.entity.Opml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.Reader
import java.util.ArrayDeque
import java.util.Deque

class OpmlParser {
    fun parse(reader: Reader): Opml {
        return try {
            extract(reader)
        } catch (e: XmlPullParserException) {
            throw OpmlParseException(e)
        } catch (e: IOException) {
            throw OpmlParseException(e)
        }
    }

    private fun extract(reader: Reader): Opml {
        val xpp = newXmlPullParser(reader)
        val initHandler = OpmlInitHandler()
        val headHandler = OpmlHeadHandler()
        val bodyHandler = OpmlBodyHandler()
        var handler: OpmlSectionHandler<*> = initHandler
        val stack: Deque<String> = ArrayDeque()
        var startedOpml = false
        var startedHead = false
        var startedBody = false
        while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
            when (xpp.next()) {
                XmlPullParser.START_TAG -> {
                    val name = xpp.name
                    stack.push(name)
                    when (name) {
                        "head" -> {
                            if (startedHead) {
                                throw OpmlParseException("OPML documents can have only one head section")
                            }
                            handler = headHandler
                            startedHead = true
                        }

                        "body" -> {
                            if (startedBody) {
                                throw OpmlParseException("OPML documents can have only one body section")
                            }
                            handler = bodyHandler
                            startedBody = true
                        }

                        else -> {
                            handler.startTag(xpp)
                            startedOpml = true
                        }
                    }
                    ValidityCheck.requirePosition(xpp, XmlPullParser.START_TAG)
                }

                XmlPullParser.TEXT -> {
                    handler.text(xpp)
                    ValidityCheck.requirePosition(xpp, XmlPullParser.TEXT)
                }

                XmlPullParser.END_TAG -> {
                    val ended = xpp.name
                    stack.pop()
                    when (ended) {
                        "head" -> {
                            handler.endTag(xpp)
                            handler = initHandler
                        }

                        "body" -> {
                            handler.endTag(xpp)
                            handler = initHandler
                        }

                        else -> {
                            handler.endTag(xpp)
                        }
                    }
                    ValidityCheck.requirePosition(xpp, XmlPullParser.END_TAG)
                }
            }
        }
        if (!stack.isEmpty()) {
            throw OpmlParseException(String.format("XML invalid, unclosed tags %s", stack))
        } else if (!startedOpml) {
            throw OpmlParseException("XML invalid, no <opml> element")
        } else if (!startedHead) {
            throw OpmlParseException("XML invalid, no <head> element")
        } else if (!startedBody) {
            throw OpmlParseException("XML invalid, no <body> element")
        }
        return Opml(initHandler.get(), headHandler.get(), bodyHandler.get())
    }

    private fun newXmlPullParser(reader: Reader): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val xpp = factory.newPullParser()
        xpp.setInput(reader)
        return xpp
    }
}
