package com.ismartcoding.lib.apk.struct.resource

import com.ismartcoding.lib.apk.struct.ChunkHeader
import com.ismartcoding.lib.apk.struct.ChunkType
import com.ismartcoding.lib.apk.utils.Buffers
import com.ismartcoding.lib.apk.utils.ParseUtils
import com.ismartcoding.lib.apk.utils.Unsigned
import java.nio.ByteBuffer

class PackageHeader(headerSize: Int, chunkSize: Long, buffer: ByteBuffer) :
    ChunkHeader(ChunkType.TABLE_PACKAGE, headerSize, chunkSize) {
    /**
     * ResourcePackage IDs start at 1 (corresponding to the value of the package bits in a resource identifier).
     * 0 means this is not a base package.
     * uint32_t
     * 0 framework-res.apk
     * 2-9 other framework files
     * 127 application package
     * Anroid 5.0+: Shared libraries will be assigned a package ID of 0x00 at build-time.
     * At runtime, all loaded shared libraries will be assigned a new package ID.
     */
    var id = 0

    /**
     * Actual name of this package, -terminated.
     * char16_t name[128]
     */
    var name: String? = null

    /**
     * Offset to a ResStringPool_header defining the resource type symbol table.
     * If zero, this package is inheriting from another base package (overriding specific values in it).
     * uinit 32
     */
    var typeStrings = 0

    /**
     * Last index into typeStrings that is for public use by others.
     * uint32_t
     */
    var lastPublicType = 0

    /**
     * Offset to a ResStringPool_header defining the resource
     * key symbol table.  If zero, this package is inheriting from
     * another base package (overriding specific values in it).
     * uint32_t
     */
    var keyStrings = 0

    /**
     * Last index into keyStrings that is for public use by others.
     * uint32_t
     */
    var lastPublicKey = 0

    init {
        id = Unsigned.toUInt(Buffers.readUInt(buffer));
        name = ParseUtils.readStringUTF16(buffer, 128);
        typeStrings = Unsigned.ensureUInt(Buffers.readUInt(buffer));
        lastPublicType = Unsigned.ensureUInt(Buffers.readUInt(buffer));
        keyStrings = Unsigned.ensureUInt(Buffers.readUInt(buffer));
        lastPublicKey = Unsigned.ensureUInt(Buffers.readUInt(buffer));
    }
}