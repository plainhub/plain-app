package com.ismartcoding.lib.apk.struct.xml

import com.ismartcoding.lib.apk.struct.ResourceValue
import com.ismartcoding.lib.apk.struct.resource.ResourceTable
import java.util.Locale

class XmlCData {
    // The raw CDATA character data.
    var data: String? = null

    // The typed value of the character data if this is a CDATA node.
    var typedData: ResourceValue? = null

    // the final value as string
    var value: String? = null

    /**
     * get value as string
     *
     * @return
     */
    fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String {
        return if (data != null) {
            CDATA_START + data + CDATA_END
        } else {
            CDATA_START + typedData!!.toStringValue(
                resourceTable,
                locale
            ) + CDATA_END
        }
    }

    override fun toString(): String {
        return "XmlCData{" +
                "data='" + data + '\'' +
                ", typedData=" + typedData +
                '}'
    }

    companion object {
        const val CDATA_START = "<![CDATA["
        const val CDATA_END = "]]>"
    }
}