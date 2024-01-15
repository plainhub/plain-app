package com.ismartcoding.lib.apk.struct.resource

import java.util.Arrays
import java.util.Locale

class ResourceMapEntry(
    // Number of bytes in this structure. uint16_t
    size: Int,
    // uint16_t
    flags: Int,
    // Reference into ResTable_package::keyStrings identifying this entry.
    //public long keyRef;
    key: String,
    // Resource identifier of the parent mapping, or 0 if there is none.
    //ResTable_ref specifies the parent Resource, if any, of this Resource.
    // struct ResTable_ref { uint32_t ident; };
    @JvmField val parent: Long,
    // Number of name/value pairs that follow for FLAG_COMPLEX. uint32_t
    @JvmField val count: Long,
    @JvmField val resourceTableMaps: Array<ResourceTableMap?>
) : ResourceEntry(size, flags, key, null) {
    /**
     * get value as string
     *
     */
    override fun toStringValue(resourceTable: ResourceTable?, locale: Locale?): String? {
        return if (resourceTableMaps.isNotEmpty()) {
            resourceTableMaps[0].toString()
        } else {
            null
        }
    }

    override fun toString(): String {
        return "ResourceMapEntry{" +
                "parent=" + parent +
                ", count=" + count +
                ", resourceTableMaps=" + Arrays.toString(resourceTableMaps) +
                '}'
    }
}