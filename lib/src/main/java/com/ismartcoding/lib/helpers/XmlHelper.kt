package com.ismartcoding.lib.helpers

import android.util.Xml
import com.ismartcoding.lib.gsonxml.GsonXmlBuilder
import org.xmlpull.v1.XmlPullParser

object XmlHelper {
    inline fun <reified T> parseData(xml: String): T {
        val bodyXml = extractFirstChildOfBody(xml)
        return decodeXml<T>(bodyXml)
    }

    // 提取 <Body> 标签下的第一个子节点完整 XML 字符串
    fun extractFirstChildOfBody(xml: String): String {
        val parser = Xml.newPullParser()
        parser.setInput(xml.reader())
        var eventType = parser.eventType
        var insideBody = false
        var tagName: String? = null
        val builder = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.endsWith("Body")) {
                        insideBody = true
                    } else if (insideBody && tagName == null) {
                        tagName = parser.name
                        builder.append("<${parser.name}")
                        // 加上所有属性（比如 xmlns:u）
                        for (i in 0 until parser.attributeCount) {
                            builder.append(" ${parser.getAttributeName(i)}=\"${parser.getAttributeValue(i)}\"")
                        }
                        builder.append(">")
                    } else if (insideBody && tagName != null) {
                        builder.append("<${parser.name}>")
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideBody && tagName != null) {
                        builder.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (insideBody && parser.name == tagName) {
                        builder.append("</${parser.name}>")
                        break
                    } else if (insideBody && tagName != null) {
                        builder.append("</${parser.name}>")
                    }
                }
            }
            eventType = parser.next()
        }

        return builder.toString()
    }

    inline fun <reified T> decodeXml(xml: String): T {
        return GsonXmlBuilder().create().fromXml(xml, T::class.java)
    }
}