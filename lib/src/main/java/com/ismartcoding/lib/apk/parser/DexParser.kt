package com.ismartcoding.lib.apk.parser

import com.ismartcoding.lib.apk.bean.DexClass
import com.ismartcoding.lib.apk.exception.ParserException
import com.ismartcoding.lib.apk.struct.StringPool
import com.ismartcoding.lib.apk.struct.dex.DexClassStruct
import com.ismartcoding.lib.apk.struct.dex.DexHeader
import com.ismartcoding.lib.apk.utils.Buffers
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DexParser(buffer: ByteBuffer) {
    private val buffer: ByteBuffer

    init {
        this.buffer = buffer.duplicate()
        this.buffer.order(ByteOrder.LITTLE_ENDIAN)
    }

    fun parse(): Array<DexClass?> {
        // read magic
        val magic = String(Buffers.readBytes(buffer, 8))
        if (!magic.startsWith("dex\n")) {
            return arrayOfNulls(0)
        }
        val version = magic.substring(4, 7).toInt()
        // now the version is 035
        if (version < 35) {
            // version 009 was used for the M3 releases of the Android platform (November–December 2007),
            // and version 013 was used for the M5 releases of the Android platform (February–March 2008)
            throw ParserException("Dex file version: $version is not supported")
        }

        // read header
        val header = readDexHeader()
        header.version = version

        // read string pool
        val stringOffsets = readStringPool(header.stringIdsOff, header.stringIdsSize)

        // read types
        val typeIds = readTypes(header.typeIdsOff, header.typeIdsSize)

        // read classes
        val dexClassStructs = readClass(
            header.classDefsOff,
            header.classDefsSize
        )
        val stringpool = readStrings(stringOffsets)
        val types = arrayOfNulls<String>(typeIds.size)
        for (i in typeIds.indices) {
            types[i] = stringpool[typeIds[i]]
        }
        val dexClasses = arrayOfNulls<DexClass>(dexClassStructs.size)
        for (i in dexClassStructs.indices) {
            val dexClassStruct = dexClassStructs[i]
            var superClass: String? = null
            if (dexClassStruct!!.superclassIdx != NO_INDEX) {
                superClass = types[dexClassStruct.superclassIdx]
            }
            dexClasses[i] = DexClass(
                header,
                types[dexClassStruct.classIdx]!!,
                superClass,
                dexClassStruct.accessFlags
            )
        }
        return dexClasses
    }

    /**
     * read class info.
     */
    private fun readClass(classDefsOff: Long, classDefsSize: Int): Array<DexClassStruct?> {
        Buffers.position(buffer, classDefsOff)
        val dexClassStructs = arrayOfNulls<DexClassStruct>(classDefsSize)
        for (i in 0 until classDefsSize) {
            val dexClassStruct = DexClassStruct()
            dexClassStruct.classIdx = buffer.int
            dexClassStruct.accessFlags = buffer.int
            dexClassStruct.superclassIdx = buffer.int
            dexClassStruct.interfacesOff = Buffers.readUInt(buffer)
            dexClassStruct.sourceFileIdx = buffer.int
            dexClassStruct.annotationsOff = Buffers.readUInt(buffer)
            dexClassStruct.classDataOff = Buffers.readUInt(buffer)
            dexClassStruct.staticValuesOff = Buffers.readUInt(buffer)
            dexClassStructs[i] = dexClassStruct
        }
        return dexClassStructs
    }

    /**
     * read types.
     */
    private fun readTypes(typeIdsOff: Long, typeIdsSize: Int): IntArray {
        Buffers.position(buffer, typeIdsOff)
        val typeIds = IntArray(typeIdsSize)
        for (i in 0 until typeIdsSize) {
            typeIds[i] = Buffers.readUInt(buffer).toInt()
        }
        return typeIds
    }

    /**
     * read string pool for dex file.
     * dex file string pool diff a bit with binary xml file or resource table.
     *
     * @param offsets
     * @return
     * @throws IOException
     */
    private fun readStrings(offsets: LongArray): StringPool {
        // read strings.
        // buffer some apk, the strings' offsets may not well ordered. we sort it first
        val entries = arrayOfNulls<StringPoolEntry>(offsets.size)
        for (i in offsets.indices) {
            entries[i] = StringPoolEntry(i, offsets[i])
        }
        var lastStr: String? = null
        var lastOffset: Long = -1
        val stringpool = StringPool(offsets.size)
        for (entry in entries) {
            if (entry!!.offset == lastOffset) {
                stringpool[entry.idx] = lastStr
                continue
            }
            Buffers.position(buffer, entry.offset)
            lastOffset = entry.offset
            val str = readString()
            lastStr = str
            stringpool[entry.idx] = str
        }
        return stringpool
    }

    /*
     * read string identifiers list.
     */
    private fun readStringPool(stringIdsOff: Long, stringIdsSize: Int): LongArray {
        Buffers.position(buffer, stringIdsOff)
        val offsets = LongArray(stringIdsSize)
        for (i in 0 until stringIdsSize) {
            offsets[i] = Buffers.readUInt(buffer)
        }
        return offsets
    }

    /**
     * read dex encoding string.
     */
    private fun readString(): String {
        // the length is char len, not byte len
        val strLen = readVarInts()
        return readString(strLen)
    }

    /**
     * read Modified UTF-8 encoding str.
     *
     * @param strLen the java-utf16-char len, not strLen nor bytes len.
     */
    private fun readString(strLen: Int): String {
        val chars = CharArray(strLen)
        for (i in 0 until strLen) {
            val a = Buffers.readUByte(buffer)
            if (a.toInt() and 0x80 == 0) {
                // ascii char
                chars[i] = Char(a.toUShort())
            } else if (a.toInt() and 0xe0 == 0xc0) {
                // read one more
                val b = Buffers.readUByte(buffer)
                chars[i] = (a.toInt() and 0x1F shl 6 or (b.toInt() and 0x3F)).toChar()
            } else if (a.toInt() and 0xf0 == 0xe0) {
                val b = Buffers.readUByte(buffer)
                val c = Buffers.readUByte(buffer)
                chars[i] =
                    (a.toInt() and 0x0F shl 12 or (b.toInt() and 0x3F shl 6) or (c.toInt() and 0x3F)).toChar()
            } else if (a.toInt() and 0xf0 == 0xf0) {
                //throw new UTFDataFormatException();
            } else {
                //throw new UTFDataFormatException();
            }
            if (chars[i].code == 0) {
                // the end of string.
            }
        }
        return String(chars)
    }

    /**
     * read varints.
     *
     * @return
     * @throws IOException
     */
    private fun readVarInts(): Int {
        var i = 0
        var count = 0
        var s: Short
        do {
            if (count > 4) {
                throw ParserException("read varints error.")
            }
            s = Buffers.readUByte(buffer)
            i = i or (s.toInt() and 0x7f shl count * 7)
            count++
        } while (s.toInt() and 0x80 != 0)
        return i
    }

    private fun readDexHeader(): DexHeader {

        // check sum. skip
        buffer.int

        // signature skip
        Buffers.readBytes(buffer, DexHeader.kSHA1DigestLen)
        val header = DexHeader()
        header.fileSize = Buffers.readUInt(buffer)
        header.headerSize = Buffers.readUInt(buffer)

        // skip?
        Buffers.readUInt(buffer)

        // static link data
        header.linkSize = Buffers.readUInt(buffer)
        header.linkOff = Buffers.readUInt(buffer)

        // the map data is just the same as dex header.
        header.mapOff = Buffers.readUInt(buffer)
        header.stringIdsSize = buffer.int
        header.stringIdsOff = Buffers.readUInt(buffer)
        header.typeIdsSize = buffer.int
        header.typeIdsOff = Buffers.readUInt(buffer)
        header.protoIdsSize = buffer.int
        header.protoIdsOff = Buffers.readUInt(buffer)
        header.fieldIdsSize = buffer.int
        header.fieldIdsOff = Buffers.readUInt(buffer)
        header.methodIdsSize = buffer.int
        header.methodIdsOff = Buffers.readUInt(buffer)
        header.classDefsSize = buffer.int
        header.classDefsOff = Buffers.readUInt(buffer)
        header.dataSize = buffer.int
        header.dataOff = Buffers.readUInt(buffer)
        Buffers.position(buffer, header.headerSize)
        return header
    }

    companion object {
        private const val NO_INDEX = -0x1
    }
}