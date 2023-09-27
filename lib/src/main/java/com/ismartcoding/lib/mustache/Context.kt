package com.ismartcoding.lib.mustache

class Context(val data: Any, val parent: Context?, val index: Int, val onFirst: Boolean, val onLast: Boolean) {
    fun nest(data: Any): Context {
        return Context(data, this, index, onFirst, onLast)
    }

    fun nest(
        data: Any,
        index: Int,
        onFirst: Boolean,
        onLast: Boolean,
    ): Context {
        return Context(data, this, index, onFirst, onLast)
    }
}
