package com.ismartcoding.lib.apk.struct.xml

import com.ismartcoding.lib.apk.struct.ResourceValue
import com.ismartcoding.lib.apk.struct.resource.ResourceTable
import com.ismartcoding.lib.apk.utils.ResourceLoader
import java.util.Locale

class Attribute (
    @JvmField val namespace: String, @JvmField val name: String,
    /**
     * The original raw string value of Attribute
     */
    @JvmField val rawValue: String?,
    /**
     * Processed typed value of Attribute
     */
    @JvmField val typedValue: ResourceValue?
){
    /**
     * the final value as string
     */
    @JvmField
    var value: String? = null
    fun toStringValue(resourceTable: ResourceTable, locale: Locale): String? {
        return if (rawValue != null) {
            rawValue
        } else {
            val typedValue = typedValue
            if (typedValue != null) {
                typedValue.toStringValue(resourceTable, locale)
            } else {
                // something happen;
                ""
            }
        }
    }

    object AttrIds {
        private val ids = ResourceLoader.loadSystemAttrIds()

        @JvmStatic
        fun getString(id: Long): String {
            return ids[id.toInt()] ?: "AttrId:0x${java.lang.Long.toHexString(id)}"
        }
    }

    override fun toString(): String {
        return "Attribute{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                '}'
    }
}