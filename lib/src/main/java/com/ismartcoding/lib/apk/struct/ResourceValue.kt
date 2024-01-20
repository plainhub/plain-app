package com.ismartcoding.lib.apk.struct

import com.ismartcoding.lib.apk.struct.resource.Densities
import com.ismartcoding.lib.apk.struct.resource.ResourceEntry
import com.ismartcoding.lib.apk.struct.resource.ResourceTable
import com.ismartcoding.lib.apk.struct.resource.TypeSpec
import com.ismartcoding.lib.apk.utils.Locales
import java.util.Locale

abstract class ResourceValue protected constructor(protected val value: Int) {
    /**
     * get value as string.
     */
    abstract fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String?
    private class DecimalResourceValue(value: Int) : ResourceValue(value) {
        override fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String {
            return value.toString()
        }
    }

    private class HexadecimalResourceValue(value: Int) : ResourceValue(value) {
        override fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String {
            return "0x" + Integer.toHexString(value)
        }
    }

    private class BooleanResourceValue(value: Int) : ResourceValue(value) {
        override fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String {
            return (value != 0).toString()
        }
    }

    private class StringResourceValue(value: Int, private val stringPool: StringPool?) :
        ResourceValue(value) {
        override fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String? {
            return if (value >= 0) {
                stringPool!![value]
            } else {
                null
            }
        }

        override fun toString(): String {
            return value.toString() + ":" + stringPool!![value]
        }
    }

    /**
     * ReferenceResource ref one another resources, and may has different value for different resource config(locale, density, etc)
     */
    class ReferenceResourceValue(value: Int) : ResourceValue(value) {
        override fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String? {
            val resourceId = referenceResourceId
            // android system styles.
            if (resourceId > AndroidConstants.SYS_STYLE_ID_START && resourceId < AndroidConstants.SYS_STYLE_ID_END) {
                return "@android:style/" + ResourceTable.sysStyle[resourceId.toInt()]
            }
            val raw = "resourceId:0x" + java.lang.Long.toHexString(resourceId)
            if (resourceTable == null) {
                return raw
            }
            val resources = resourceTable.getResourcesById(resourceId)
            // read from type resource
            var selected: ResourceEntry? = null
            var typeSpec: TypeSpec? = null
            var currentLocalMatchLevel = -1
            var currentDensityLevel = -1
            for (resource in resources) {
                val type = resource.type
                typeSpec = resource.typeSpec
                val resourceEntry = resource.resourceEntry
                val localMatchLevel = Locales.match(locale, type.locale)
                val densityLevel = densityLevel(type.density)
                if (localMatchLevel > currentLocalMatchLevel) {
                    selected = resourceEntry
                    currentLocalMatchLevel = localMatchLevel
                    currentDensityLevel = densityLevel
                } else if (densityLevel > currentDensityLevel) {
                    selected = resourceEntry
                    currentDensityLevel = densityLevel
                }
            }
            val result: String?
            result = if (selected == null) {
                raw
            } else if (locale == null) {
                "@" + typeSpec!!.name + "/" + selected.key
            } else {
                selected.toStringValue(resourceTable, locale)
            }
            return result
        }

        val referenceResourceId: Long
            get() = value.toLong() and 0xFFFFFFFFL

        companion object {
            private fun densityLevel(density: Int): Int {
                return if (density == Densities.ANY || density == Densities.NONE) {
                    -1
                } else density
            }
        }
    }

    private class NullResourceValue private constructor() : ResourceValue(-1) {
        override fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String? {
            return ""
        }

        companion object {
            val instance = NullResourceValue()
        }
    }

    private class RGBResourceValue(value: Int, private val len: Int) : ResourceValue(value) {
        override fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String {
            val sb = StringBuilder()
            for (i in len / 2 - 1 downTo 0) {
                sb.append(Integer.toHexString(value shr i * 8 and 0xff))
            }
            return sb.toString()
        }
    }

    private class DimensionValue(value: Int) : ResourceValue(value) {
        override fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String {
            val unit = (value and 0xff).toShort()
            val unitStr: String = when (unit) {
                ResValue.ResDataCOMPLEX.UNIT_MM -> "mm"
                ResValue.ResDataCOMPLEX.UNIT_PX -> "px"
                ResValue.ResDataCOMPLEX.UNIT_DIP -> "dp"
                ResValue.ResDataCOMPLEX.UNIT_SP -> "sp"
                ResValue.ResDataCOMPLEX.UNIT_PT -> "pt"
                ResValue.ResDataCOMPLEX.UNIT_IN -> "in"
                else -> "unknown unit:0x" + Integer.toHexString(unit.toInt())
            }
            return (value shr 8).toString() + unitStr
        }
    }

    private class FractionValue(value: Int) : ResourceValue(value) {
        override fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String {
            // The low-order 4 bits of the data value specify the type of the fraction
            val type = (value and 0xf).toShort()
            val pstr: String
            pstr = when (type) {
                ResValue.ResDataCOMPLEX.UNIT_FRACTION -> "%"
                ResValue.ResDataCOMPLEX.UNIT_FRACTION_PARENT -> "%p"
                else -> "unknown type:0x" + Integer.toHexString(type.toInt())
            }
            val f = java.lang.Float.intBitsToFloat(value shr 4)
            return f.toString() + pstr
        }
    }

    private class RawValue(value: Int, private val dataType: Short) : ResourceValue(value) {
        override fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String {
            return "{" + dataType + ":" + (value.toLong() and 0xFFFFFFFFL) + "}"
        }
    }

    companion object {
        fun decimal(value: Int): ResourceValue {
            return DecimalResourceValue(value)
        }

        fun hexadecimal(value: Int): ResourceValue {
            return HexadecimalResourceValue(value)
        }

        fun bool(value: Int): ResourceValue {
            return BooleanResourceValue(value)
        }

        fun string(value: Int, stringPool: StringPool?): ResourceValue {
            return StringResourceValue(value, stringPool)
        }

        fun reference(value: Int): ResourceValue {
            return ReferenceResourceValue(value)
        }

        fun nullValue(): ResourceValue {
            return NullResourceValue.instance
        }

        fun rgb(value: Int, len: Int): ResourceValue {
            return RGBResourceValue(value, len)
        }

        fun dimension(value: Int): ResourceValue {
            return DimensionValue(value)
        }

        fun fraction(value: Int): ResourceValue {
            return FractionValue(value)
        }

        fun raw(value: Int, type: Short): ResourceValue {
            return RawValue(value, type)
        }
    }
}