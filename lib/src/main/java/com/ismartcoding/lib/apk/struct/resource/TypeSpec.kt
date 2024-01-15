package com.ismartcoding.lib.apk.struct.resource

class TypeSpec(
    @JvmField val header: TypeSpecHeader,
    @JvmField val flags: LongArray,
    @JvmField val typeSpecName: String
) {
    private var entryFlags: LongArray = flags
    var name: String? = null
    var id: Short = header.id.toShort()

    init {
        name = typeSpecName
    }

    fun exists(id: Int): Boolean {
        return id < entryFlags.size
    }

    override fun toString(): String {
        return "TypeSpec{" +
                "name='" + name + '\'' +
                ", id=" + id +
                '}'
    }
}