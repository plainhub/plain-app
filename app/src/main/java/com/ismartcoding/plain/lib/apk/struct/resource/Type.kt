package com.ismartcoding.plain.lib.apk.struct.resource

import com.ismartcoding.plain.lib.apk.struct.StringPool
import com.ismartcoding.plain.lib.apk.utils.Buffers.position
import com.ismartcoding.plain.lib.apk.utils.Buffers.readUInt
import com.ismartcoding.plain.lib.apk.utils.Buffers.readUShort
import com.ismartcoding.plain.lib.apk.utils.ParseUtils.readResValue
import com.ismartcoding.plain.lib.apk.utils.Buffers
import com.ismartcoding.plain.lib.apk.utils.ParseUtils
import java.nio.ByteBuffer
import java.util.Locale


class Type(@JvmField val header: TypeHeader) {
    var name: String? = null
    var id: Short = header.id.toShort()
    var locale = Locale(header.config.language, header.config.country)
    var keyStringPool: StringPool? = null
    lateinit var buffer: ByteBuffer
    lateinit var offsets: LongArray
    var stringPool: StringPool? = null
    val density: Int = header.config.density.toInt()

    fun getResourceEntry(id: Int): ResourceEntry? {
        if (id >= offsets.size) {
            return null
        }
        if (offsets[id] == TypeHeader.NO_ENTRY) {
            return null
        }

        // read Resource Entries
        Buffers.position(buffer, offsets[id])
        return readResourceEntry()
    }

    private fun readResourceEntry(): ResourceEntry? {
        val beginPos = buffer.position().toLong()
        // size is always 8(simple), or 16(complex)
        // size is always 8(simple), or 16(complex)
        val resourceEntrySize = Buffers.readUShort(buffer)
        val resourceEntryFlags = Buffers.readUShort(buffer)
        val keyRef = buffer.int.toLong()
        val resourceEntryKey = keyStringPool?.get(keyRef.toInt()) ?: return null
        return if (resourceEntryFlags and ResourceEntry.FLAG_COMPLEX != 0) {
            // Resource identifier of the parent mapping, or 0 if there is none.
            val parent = Buffers.readUInt(buffer)
            val count = Buffers.readUInt(buffer)
            //            resourceMapEntry.setParent(parent);
            //            resourceMapEntry.setCount(count);
            Buffers.position(buffer, beginPos + resourceEntrySize)
            //An individual complex Resource entry comprises an entry immediately followed by one or more fields.
            val resourceTableMaps = arrayOfNulls<ResourceTableMap>(count.toInt())
            for (i in 0 until count) {
                resourceTableMaps[i.toInt()] = readResourceTableMap()
            }
            ResourceMapEntry(
                resourceEntrySize,
                resourceEntryFlags,
                resourceEntryKey,
                parent,
                count,
                resourceTableMaps
            )
        } else {
            Buffers.position(buffer, beginPos + resourceEntrySize)
            val resourceEntryValue = ParseUtils.readResValue(buffer, stringPool)
            ResourceEntry(
                resourceEntrySize, resourceEntryFlags, resourceEntryKey, resourceEntryValue
            )
        }
    }

    private fun readResourceTableMap(): ResourceTableMap {
        val resourceTableMap = ResourceTableMap()
        resourceTableMap.nameRef = Buffers.readUInt(buffer)
        resourceTableMap.resValue = ParseUtils.readResValue(buffer, stringPool)
        if (resourceTableMap.nameRef and 0x02000000L != 0L) {
            //read arrays
        } else if (resourceTableMap.nameRef and 0x01000000L != 0L) {
            // read attrs
        } else {
        }
        return resourceTableMap
    }

    override fun toString(): String {
        return "Type{name='$name', id=$id, locale=$locale}"
    }
}