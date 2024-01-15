package com.ismartcoding.lib.apk.struct.xml

class Attributes(size: Int) {
    /**
     * return all attributes
     */
    @JvmField
    val attributes: Array<Attribute?>

    init {
        attributes = arrayOfNulls(size)
    }

    operator fun set(i: Int, attribute: Attribute?) {
        attributes[i] = attribute
    }

    operator fun get(name: String): Attribute? {
        //TODO this is an inefficient search. Should probably be using HashMap
        var result: Attribute? = null
        for (attribute in attributes) {
            if (attribute!!.name == name) {
                val namespace = attribute.namespace
                if (namespace.isEmpty() || namespace == "android" || namespace == "http://schemas.android.com/apk/res/android") {
                    //prefer default namespace of android.
                    result = attribute
                    break
                }
                if (result == null)
                    result = attribute
            }
        }
        return result
    }

    /**
     * Get attribute with name, return value as string
     */
    fun getString(name: String): String? {
        return this[name]?.value
    }

    fun size(): Int {
        return attributes.size
    }

    fun getBoolean(name: String, b: Boolean): Boolean {
        val value = getString(name)
        return if (value == null) b else java.lang.Boolean.parseBoolean(value)
    }

    fun getInt(name: String): Int? {
        val value = getString(name) ?: return null
        return if (value.startsWith("0x")) {
            Integer.valueOf(value.substring(2), 16)
        } else Integer.valueOf(value)
    }

    fun getLong(name: String): Long? {
        val value = getString(name) ?: return null
        return if (value.startsWith("0x")) {
            java.lang.Long.valueOf(value.substring(2), 16)
        } else java.lang.Long.valueOf(value)
    }
}