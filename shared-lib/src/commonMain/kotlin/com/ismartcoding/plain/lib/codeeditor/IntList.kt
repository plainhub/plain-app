package com.ismartcoding.plain.lib.codeeditor

/** Primitive growable int array; avoids boxing of ArrayList&lt;Int&gt; for million-entry line indexes. */
class IntList(initialCapacity: Int = 1024) {
    private var array = IntArray(initialCapacity.coerceAtLeast(16))
    var size: Int = 0
        private set

    fun add(value: Int) {
        ensureCapacity(size + 1)
        array[size] = value
        size++
    }

    fun get(index: Int): Int = array[index]

    fun set(index: Int, value: Int) {
        array[index] = value
    }

    fun insert(index: Int, value: Int) {
        ensureCapacity(size + 1)
        copyWithin(index, index + 1, size)
        array[index] = value
        size++
    }

    fun removeRange(from: Int, to: Int) {
        copyWithin(to, from, size)
        size -= (to - from)
    }

    fun addAll(other: IntList, from: Int = 0, to: Int = other.size) {
        ensureCapacity(size + (to - from))
        for (i in from until to) {
            array[size] = other.get(i)
            size++
        }
    }

    fun snapshot(): IntList {
        val copy = IntList(size)
        array.copyInto(copy.array, 0, 0, size)
        copy.size = size
        return copy
    }

    fun clear() {
        size = 0
    }

    private fun copyWithin(srcFrom: Int, dstFrom: Int, srcTo: Int) {
        if (srcFrom == srcTo) return
        array.copyInto(array, dstFrom, srcFrom, srcTo)
    }

    private fun ensureCapacity(min: Int) {
        if (min <= array.size) return
        var newCap = array.size
        while (newCap < min) newCap *= 2
        array = array.copyOf(newCap)
    }
}

/** Growable bit set with positional insert/delete, used for per-line CRLF flags. */
class BitList(initialCapacity: Int = 1024) {
    private var words = LongArray((initialCapacity / 64 + 1).coerceAtLeast(1))
    var size: Int = 0
        private set

    fun get(index: Int): Boolean {
        val w = index ushr 6
        if (w >= words.size) return false
        return (words[w] and (1L shl (index and 63))) != 0L
    }

    fun set(index: Int, value: Boolean) {
        val w = index ushr 6
        ensureWords(w + 1)
        if (value) words[w] = words[w] or (1L shl (index and 63)) else words[w] = words[w] and (1L shl (index and 63)).inv()
    }

    fun add(value: Boolean) {
        set(size, value)
        size++
    }

    fun insert(index: Int, value: Boolean) {
        require(index in 0..size) { "BitList.insert index $index out of [0, $size]" }
        // Shift bits [index, size) right by one.
        ensureWords(((size + 1) ushr 6) + 1)
        for (i in size downTo index + 1) {
            set(i, get(i - 1))
        }
        set(index, value)
        size++
    }

    fun removeRange(from: Int, to: Int) {
        require(from in 0..size && to >= from && to <= size)
        for (i in from until size - (to - from)) {
            set(i, get(i + (to - from)))
        }
        size -= (to - from)
    }

    fun snapshot(): BitList {
        val copy = BitList(size)
        copy.ensureWords(words.size)
        words.copyInto(copy.words, 0, 0, words.size)
        copy.size = size
        return copy
    }

    fun clear() {
        words.fill(0L)
        size = 0
    }

    private fun ensureWords(wordCount: Int) {
        if (wordCount <= words.size) return
        words = words.copyOf(maxOf(wordCount, words.size * 2))
    }
}
