package com.ismartcoding.lib.apk.utils

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory

class ResourceFetcher {
    // from https://android.googlesource.com/platform/frameworks/base/+/master/core/res/res/values/public.xml
    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    private fun fetchSystemAttrIds() {
        val url =
            "https://android.googlesource.com/platform/frameworks/base/+/master/core/res/res/values/public.xml"
        val html = getUrl(url)
        val xml = retrieveCode(html)
        xml?.let { parseAttributeXml(it) }
    }

    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    private fun parseAttributeXml(xml: String) {
        val factory = SAXParserFactory.newInstance()
        val parser = factory.newSAXParser()
        val attrIds: MutableList<Pair<Int, String>> = ArrayList()
        val dh: DefaultHandler = object : DefaultHandler() {
            @Throws(SAXException::class)
            override fun startElement(
                uri: String, localName: String, qName: String,
                attributes: Attributes
            ) {
                if (qName != "public") {
                    return
                }
                val type = attributes.getValue("type") ?: return
                if (type == "attr") {
                    //attr ids.
                    var idStr = attributes.getValue("id") ?: return
                    val name = attributes.getValue("name")
                    if (idStr.startsWith("0x")) {
                        idStr = idStr.substring(2)
                    }
                    val id = idStr.toInt(16)
                    attrIds.add(Pair(id, name))
                }
            }
        }
        parser.parse(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)), dh)
        for (pair in attrIds) {
            println(String.format("%s=%d", pair.right, pair.left))
        }
    }

    // the android system r style.
    // see http://developer.android.com/reference/android/R.style.html
    // from https://android.googlesource.com/platform/frameworks/base/+/master/api/current.txt r.style section
    @Throws(IOException::class)
    private fun fetchSystemStyle() {
        val url = "https://android.googlesource.com/platform/frameworks/base/+/master/api/current.txt"
        val html = getUrl(url)
        val code = retrieveCode(html)
        if (code == null) {
            System.err.println("code area not found")
            return
        }
        val begin = code.indexOf("R.style")
        val end = code.indexOf("}", begin)
        val styleCode = code.substring(begin, end)
        val lines = styleCode.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            var line = line.trim { it <= ' ' }
            if (line.startsWith("field public static final")) {
                line = Strings.substringBefore(line, ";").replace("deprecated ", "")
                    .substring("field public static final int ".length).replace("_", ".")
                println(line)
            }
        }
    }

    @Throws(IOException::class)
    private fun getUrl(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.readTimeout = 10000
            conn.connectTimeout = 10000
            val bytes = Inputs.readAllAndClose(conn.inputStream)
            String(bytes, StandardCharsets.UTF_8)
        } finally {
            conn.disconnect()
        }
    }

    /** @noinspection AlibabaAvoidPatternCompileInMethod
     */
    private fun retrieveCode(html: String): String? {
        val matcher = Pattern.compile("<ol class=\"prettyprint\">(.*?)</ol>").matcher(html)
        return if (matcher.find()) {
            val codeHtml = matcher.group(1)
            codeHtml?.replace("</li>", "\n")?.replace("<[^>]+>".toRegex(), "")?.replace("&lt;", "<")
                ?.replace("&quot;", "\"")?.replace("&gt;", ">")
        } else {
            null
        }
    }

    companion object {
        @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val fetcher = ResourceFetcher()
            fetcher.fetchSystemAttrIds()
            //fetcher.fetchSystemStyle();
        }
    }
}