package com.ismartcoding.lib.apk.struct

object ChunkType {
    const val NULL = 0x0000
    const val STRING_POOL = 0x0001
    const val TABLE = 0x0002
    const val XML = 0x0003

    // Chunk types in XML
    const val XML_FIRST_CHUNK = 0x0100
    const val XML_START_NAMESPACE = 0x0100
    const val XML_END_NAMESPACE = 0x0101
    const val XML_START_ELEMENT = 0x0102
    const val XML_END_ELEMENT = 0x0103
    const val XML_CDATA = 0x0104
    const val XML_LAST_CHUNK = 0x017f

    // This contains a uint32_t array mapping strings in the string
    // pool back to resource identifiers.  It is optional.
    const val XML_RESOURCE_MAP = 0x0180

    // Chunk types in RES_TABLE_TYPE
    const val TABLE_PACKAGE = 0x0200
    const val TABLE_TYPE = 0x0201
    const val TABLE_TYPE_SPEC = 0x0202

    // android5.0+
    // DynamicRefTable
    const val TABLE_LIBRARY = 0x0203

    //TODO: fix this later. Do not found definition for chunk type 0x0204 in android source yet...
    const val UNKNOWN_YET = 0x0204

    /**
     * TODO: handle the chunks types below
     * https://github.com/hsiafan/apk-parser/issues/96#issuecomment-500275300 https://github.com/AndroidDeveloperLB/apk-parser/issues/1
     */
    const val TABLE_OVER_LAYABLE = 0x0204
    const val TABLE_STAGED_ALIAS = 0x0206
}