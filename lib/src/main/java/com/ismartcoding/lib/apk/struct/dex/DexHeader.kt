package com.ismartcoding.lib.apk.struct.dex

class DexHeader {
    // includes version number. 8 bytes.
    //public short magic;
    var version = 0

    // adler32 checksum. u4
    //public long checksum;
    // SHA-1 hash len = kSHA1DigestLen
    lateinit var signature: ByteArray

    // length of entire file. u4
    var fileSize: Long = 0

    // len of header.offset to start of next section. u4
    var headerSize: Long = 0

    // u4
    //public long endianTag;
    // u4
    var linkSize: Long = 0

    // u4
    var linkOff: Long = 0

    // u4
    var mapOff: Long = 0

    // u4
    var stringIdsSize = 0

    // u4
    var stringIdsOff: Long = 0

    // u4
    var typeIdsSize = 0

    // u4
    var typeIdsOff: Long = 0

    // u4
    var protoIdsSize = 0

    // u4
    var protoIdsOff: Long = 0

    // u4
    var fieldIdsSize = 0

    // u4
    var fieldIdsOff: Long = 0

    // u4
    var methodIdsSize = 0

    // u4
    var methodIdsOff: Long = 0

    // u4
    var classDefsSize = 0

    // u4
    var classDefsOff: Long = 0

    // u4
    var dataSize = 0

    // u4
    var dataOff: Long = 0

    companion object {
        const val kSHA1DigestLen = 20
        const val kSHA1DigestOutputLen = kSHA1DigestLen * 2 + 1
    }
}