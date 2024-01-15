package com.ismartcoding.lib.apk.struct.resource

import com.ismartcoding.lib.apk.struct.ResourceValue
import java.util.Locale

open class ResourceEntry(
    // Number of bytes in this structure. uint16_t
    @JvmField val size: Int,
    // uint16_t
    @JvmField val flags: Int,
    // Reference into ResTable_package::keyStrings identifying this entry.
    //public long keyRef;
    @JvmField val key: String,
    // the resvalue following this resource entry.
    @JvmField val value: ResourceValue?
) {
    /**
     * get value as string
     *
     */
    open fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String? {
        return if (value != null) {
            value.toStringValue(resourceTable, locale)
        } else {
            "null"
        }
    }

    override fun toString(): String {
        return "ResourceEntry{" +
                "size=" + size +
                ", flags=" + flags +
                ", key='" + key + '\'' +
                ", value=" + value +
                '}'
    }

    companion object {
        // If set, this is a complex entry, holding a set of name/value
        // mappings.  It is followed by an array of ResTable_map structures.
        const val FLAG_COMPLEX = 0x0001

        // If set, this resource has been declared public, so libraries
        // are allowed to reference it.
        const val FLAG_PUBLIC = 0x0002
    }
}