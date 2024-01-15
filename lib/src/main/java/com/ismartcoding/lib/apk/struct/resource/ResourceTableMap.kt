package com.ismartcoding.lib.apk.struct.resource

import com.ismartcoding.lib.apk.struct.ResourceValue

class ResourceTableMap {
    // ...elided
    // ResTable_ref; unit32
    var nameRef: Long = 0
    var resValue: ResourceValue? = null
    var data: String? = null
    override fun toString(): String {
        return data!!
    }

    object MapAttr {
        const val TYPE = 0x01000000 or 0

        // For integral attributes; this is the minimum value it can hold.
        const val MIN = 0x01000000 or (1 and 0xFFFF)

        // For integral attributes; this is the maximum value it can hold.
        const val MAX = 0x01000000 or (2 and 0xFFFF)

        // Localization of this resource is can be encouraged or required with
        // an aapt flag if this is set
        const val L10N = 0x01000000 or (3 and 0xFFFF)

        // for plural support; see android.content.res.PluralRules#attrForQuantity(int)
        const val OTHER = 0x01000000 or (4 and 0xFFFF)
        const val ZERO = 0x01000000 or (5 and 0xFFFF)
        const val ONE = 0x01000000 or (6 and 0xFFFF)
        const val TWO = 0x01000000 or (7 and 0xFFFF)
        const val FEW = 0x01000000 or (8 and 0xFFFF)
        const val MANY = 0x01000000 or (9 and 0xFFFF)
        fun makeArray(entry: Int): Int {
            return 0x02000000 or (entry and 0xFFFF)
        }
    }

    object AttributeType {
        // No type has been defined for this attribute; use generic
        // type handling.  The low 16 bits are for types that can be
        // handled generically; the upper 16 require additional information
        // in the bag so can not be handled generically for ANY.
        const val ANY = 0x0000FFFF

        // Attribute holds a references to another resource.
        const val REFERENCE = 1

        // Attribute holds a generic string.
        const val STRING = 1 shl 1

        // Attribute holds an integer value.  ATTR_MIN and ATTR_MIN can
        // optionally specify a constrained range of possible integer values.
        const val INTEGER = 1 shl 2

        // Attribute holds a boolean integer.
        const val BOOLEAN = 1 shl 3

        // Attribute holds a color value.
        const val COLOR = 1 shl 4

        // Attribute holds a floating point value.
        const val FLOAT = 1 shl 5

        // Attribute holds a dimension value; such as "20px".
        const val DIMENSION = 1 shl 6

        // Attribute holds a fraction value; such as "20%".
        const val FRACTION = 1 shl 7

        // Attribute holds an enumeration.  The enumeration values are
        // supplied as additional entries in the map.
        const val ENUM = 1 shl 16

        // Attribute holds a bitmaks of flags.  The flag bit values are
        // supplied as additional entries in the map.
        const val FLAGS = 1 shl 17
    }
}