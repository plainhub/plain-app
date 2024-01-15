package com.ismartcoding.lib.apk.struct.dex

class DexClassStruct {
    /* index into typeIds for this class. u4 */
    var classIdx = 0
    var accessFlags = 0

    /* index into typeIds for superclass. u4 */
    var superclassIdx = 0

    /* file offset to DexTypeList. u4 */
    var interfacesOff: Long = 0

    /* index into stringIds for source file name. u4 */
    var sourceFileIdx = 0

    /* file offset to annotations_directory_item. u4 */
    var annotationsOff: Long = 0

    /* file offset to class_data_item. u4 */
    var classDataOff: Long = 0

    /* file offset to DexEncodedArray. u4 */
    var staticValuesOff: Long = 0

    companion object {
        var ACC_PUBLIC = 0x1
        var ACC_PRIVATE = 0x2
        var ACC_PROTECTED = 0x4
        var ACC_STATIC = 0x8
        var ACC_FINAL = 0x10
        var ACC_SYNCHRONIZED = 0x20
        var ACC_VOLATILE = 0x40
        var ACC_BRIDGE = 0x40
        var ACC_TRANSIENT = 0x80
        var ACC_VARARGS = 0x80
        var ACC_NATIVE = 0x100
        var ACC_INTERFACE = 0x200
        var ACC_ABSTRACT = 0x400
        var ACC_STRICT = 0x800
        var ACC_SYNTHETIC = 0x1000
        var ACC_ANNOTATION = 0x2000
        var ACC_ENUM = 0x4000
        var ACC_CONSTRUCTOR = 0x10000
        var ACC_DECLARED_SYNCHRONIZED = 0x20000
    }
}