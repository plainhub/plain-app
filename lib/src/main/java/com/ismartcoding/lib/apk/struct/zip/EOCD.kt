package com.ismartcoding.lib.apk.struct.zip

/**
 * End of central directory record
 */
class EOCD {
    //    private List<String> commentList;
    //    private int signature;
    // Number of this disk
    var diskNum: Int = 0

    // Disk where central directory starts
    var cdStartDisk: Short = 0
        get() = (field.toInt() and 0xffff).toShort()

    // Number of central directory records on this disk
    var cdRecordNum: Short = 0
        get() = (field.toInt() and 0xffff).toShort()

    // Total number of central directory records
    var totalCDRecordNum: Short = 0


    // Size of central directory (bytes)
    var cdSize: Long = 0
        get() = field and 0xffffffffL

    // Offset of start of central directory, relative to start of archive
    var cdStart: Long = 0
        get() = field and 0xffffffffL

    // Comment length (n)
    var commentLen: Short = 0

    companion object {
        const val SIGNATURE = 0x06054b50
    }
}