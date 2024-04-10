package com.ismartcoding.lib.apk.struct

class StringPool(poolSize: Int) {
    private val pool: Array<String?>  = arrayOfNulls(poolSize)

    operator fun get(idx: Int): String? {
        return pool.getOrNull(idx)
    }

    operator fun set(idx: Int, value: String?) {
        pool[idx] = value
    }
}