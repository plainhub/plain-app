package com.ismartcoding.lib.gsonxml

/**
 * Array-based stack.
 * @param <T> element type
</T> */
internal class Stack<T> {
    /** Size.  */
    private var array = arrayOfNulls<Any>(32)

    /** Stack size.  */
    private var size = 0

    fun peek(): T? {
        return array[size - 1] as T?
    }

    fun size(): Int {
        return size
    }

    operator fun get(pos: Int): T? {
        return array[pos] as T?
    }

    fun drop() {
        size--
    }

    @JvmOverloads
    fun cleanup(
        count: Int,
        oldStackSize: Int = size,
    ): Int {
        val curStackSize = size
        if (oldStackSize < curStackSize) {
            for (i in oldStackSize until curStackSize) {
                array[i - count] = array[i]
            }
            size -= count
        } else {
            size -= count - oldStackSize + curStackSize
        }
        if (size < 0) {
            size = 0
        }
        return oldStackSize - count
    }

    fun fix(check: T) {
        size--
        if (size > 0 && array[size - 1] === check) {
            size--
        }
    }

    private fun ensureStack() {
        if (size == array.size) {
            val newStack = arrayOfNulls<Any>(size * 2)
            System.arraycopy(array, 0, newStack, 0, size)
            array = newStack
        }
    }

    fun push(value: T) {
        ensureStack()
        array[size++] = value
    }

    fun pushAt(
        position: Int,
        scope: T,
    ) {
        var pos = position
        if (pos < 0) {
            pos = 0
        }
        ensureStack()
        for (i in size - 1 downTo pos) {
            array[i + 1] = array[i]
        }
        array[pos] = scope
        size++
    }

    override fun toString(): String {
        val res = StringBuilder()
        for (i in 0 until size) {
            res.append(array[i]).append('>')
        }
        if (res.length > 0) {
            res.delete(res.length - 1, res.length)
        }
        return res.toString()
    }
}
