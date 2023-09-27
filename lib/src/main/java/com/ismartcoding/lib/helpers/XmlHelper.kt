package com.ismartcoding.lib.helpers

import com.ismartcoding.lib.gsonxml.GsonXmlBuilder

object XmlHelper {
    inline fun <reified T> xmlDecode(xml: String): T {
        return GsonXmlBuilder().create().fromXml(xml, T::class.java)
    }
}
