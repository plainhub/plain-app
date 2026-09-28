package com.ismartcoding.plain.lib.opml

import com.ismartcoding.plain.lib.opml.entity.Opml

class OpmlParser {
    fun parse(xml: String): Opml {
        return try {
            extract(xml)
        } catch (e: OpmlParseException) {
            throw e
        } catch (e: Exception) {
            throw OpmlParseException(e)
        }
    }

    private fun extract(xml: String): Opml {
        val xpp = SimpleXmlReader(xml)
        val initHandler = OpmlInitHandler()
        val headHandler = OpmlHeadHandler()
        val bodyHandler = OpmlBodyHandler()
        var handler: OpmlSectionHandler<*> = initHandler
        val stack = ArrayDeque<String>()
        var startedOpml = false
        var startedHead = false
        var startedBody = false
        while (xpp.eventType != SimpleXmlReader.END_DOCUMENT) {
            when (xpp.next()) {
                SimpleXmlReader.START_TAG -> {
                    val name = xpp.name
                    stack.addLast(name)
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
                    ValidityCheck.requirePosition(xpp, SimpleXmlReader.START_TAG)
                }

                SimpleXmlReader.TEXT -> {
                    handler.text(xpp)
                    ValidityCheck.requirePosition(xpp, SimpleXmlReader.TEXT)
                }

                SimpleXmlReader.END_TAG -> {
                    val ended = xpp.name
                    stack.removeLast()
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
                    ValidityCheck.requirePosition(xpp, SimpleXmlReader.END_TAG)
                }
            }
        }
        if (stack.isNotEmpty()) {
            throw OpmlParseException("XML invalid, unclosed tags $stack")
        } else if (!startedOpml) {
            throw OpmlParseException("XML invalid, no <opml> element")
        } else if (!startedHead) {
            throw OpmlParseException("XML invalid, no <head> element")
        } else if (!startedBody) {
            throw OpmlParseException("XML invalid, no <body> element")
        }
        return Opml(initHandler.get(), headHandler.get(), bodyHandler.get())
    }
}
