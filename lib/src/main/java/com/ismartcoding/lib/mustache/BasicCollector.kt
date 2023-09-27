package com.ismartcoding.lib.mustache

import com.ismartcoding.lib.mustache.Mustache.CustomContext
import com.ismartcoding.lib.mustache.Mustache.VariableFetcher

/**
 * A collector that does not use reflection and can be used with GWT.
 */
abstract class BasicCollector : ICollector {
    override fun toIterator(value: Any): Iterator<*>? {
        if (value is Iterable<*>) {
            return value.iterator()
        }
        if (value is Iterator<*>) {
            return value
        }
        if (value.javaClass.isArray) {
            val helper = arrayHelper(value)
            return object : MutableIterator<Any> {
                private val _count = helper!!.length(value)
                private var _idx = 0

                override fun hasNext(): Boolean {
                    return _idx < _count
                }

                override fun next(): Any {
                    return helper!![value, _idx++]
                }

                override fun remove() {
                    throw UnsupportedOperationException()
                }
            }
        }
        return null
    }

    override fun createFetcher(
        ctx: Any,
        name: String,
    ): VariableFetcher? {
        if (ctx is CustomContext) return CUSTOM_FETCHER
        if (ctx is Map<*, *>) return MAP_FETCHER

        // if the name looks like a number, potentially use one of our 'indexing' fetchers
        val c = name[0]
        if (c in '0'..'9') {
            if (ctx is List<*>) return LIST_FETCHER
            if (ctx is Iterator<*>) return ITER_FETCHER
            if (ctx.javaClass.isArray) return arrayHelper(ctx)
        }
        return null
    }

    /** This should return a thread-safe map, either [Collections.synchronizedMap] called on
     * a standard [Map] implementation or something like `ConcurrentHashMap`.  */
    abstract override fun <K, V> createFetcherCache(): MutableMap<K, V>

    abstract class ArrayHelper : VariableFetcher {
        @Throws(java.lang.Exception::class)
        override fun get(
            ctx: Any,
            name: String,
        ): Any {
            return try {
                get(ctx, name.toInt())
            } catch (nfe: java.lang.NumberFormatException) {
                Template.NO_FETCHER_FOUND
            } catch (e: ArrayIndexOutOfBoundsException) {
                Template.NO_FETCHER_FOUND
            }
        }

        abstract fun length(ctx: Any): Int

        abstract operator fun get(
            ctx: Any,
            index: Int,
        ): Any
    }

    companion object {
        protected fun arrayHelper(ctx: Any): ArrayHelper? {
            if (ctx is Array<*>) return OBJECT_ARRAY_HELPER
            if (ctx is BooleanArray) return BOOLEAN_ARRAY_HELPER
            if (ctx is ByteArray) return BYTE_ARRAY_HELPER
            if (ctx is CharArray) return CHAR_ARRAY_HELPER
            if (ctx is ShortArray) return SHORT_ARRAY_HELPER
            if (ctx is IntArray) return INT_ARRAY_HELPER
            if (ctx is LongArray) return LONG_ARRAY_HELPER
            if (ctx is FloatArray) return FLOAT_ARRAY_HELPER
            return if (ctx is DoubleArray) DOUBLE_ARRAY_HELPER else null
        }

        protected val CUSTOM_FETCHER =
            object : VariableFetcher {
                @Throws(Exception::class)
                override fun get(
                    ctx: Any,
                    name: String,
                ): Any {
                    val custom = ctx as CustomContext
                    val `val` = custom[name]
                    return `val` ?: Template.NO_FETCHER_FOUND
                }

                override fun toString(): String {
                    return "CUSTOM_FETCHER"
                }
            }
        protected val MAP_FETCHER =
            object : VariableFetcher {
                @Throws(Exception::class)
                override fun get(
                    ctx: Any,
                    name: String,
                ): Any {
                    val map = ctx as Map<*, *>
                    if (map.containsKey(name)) return map[name]!!
                    // special case to allow map entry set to be iterated over
                    return if ("entrySet" == name) map.entries else Template.NO_FETCHER_FOUND
                }

                override fun toString(): String {
                    return "MAP_FETCHER"
                }
            }
        protected val LIST_FETCHER =
            object : VariableFetcher {
                @Throws(Exception::class)
                override fun get(
                    ctx: Any,
                    name: String,
                ): Any {
                    return try {
                        (ctx as List<*>)[name.toInt()]!!
                    } catch (nfe: NumberFormatException) {
                        Template.NO_FETCHER_FOUND
                    } catch (e: IndexOutOfBoundsException) {
                        Template.NO_FETCHER_FOUND
                    }
                }

                override fun toString(): String {
                    return "LIST_FETCHER"
                }
            }
        protected val ITER_FETCHER =
            object : VariableFetcher {
                @Throws(Exception::class)
                override fun get(
                    ctx: Any,
                    name: String,
                ): Any {
                    return try {
                        val iter = ctx as Iterator<*>
                        var ii = 0
                        val ll = name.toInt()
                        while (ii < ll) {
                            iter.next()
                            ii++
                        }
                        iter.next()!!
                    } catch (nfe: NumberFormatException) {
                        Template.NO_FETCHER_FOUND
                    } catch (e: NoSuchElementException) {
                        Template.NO_FETCHER_FOUND
                    }
                }

                override fun toString(): String {
                    return "ITER_FETCHER"
                }
            }
        protected val OBJECT_ARRAY_HELPER =
            object : ArrayHelper() {
                override fun get(
                    ctx: Any,
                    index: Int,
                ): Any {
                    return (ctx as Array<*>)[index]!!
                }

                override fun length(ctx: Any): Int {
                    return (ctx as Array<*>).size
                }
            }
        protected val BOOLEAN_ARRAY_HELPER =
            object : ArrayHelper() {
                override fun get(
                    ctx: Any,
                    index: Int,
                ): Any {
                    return (ctx as BooleanArray)[index]
                }

                override fun length(ctx: Any): Int {
                    return (ctx as BooleanArray).size
                }
            }
        protected val BYTE_ARRAY_HELPER =
            object : ArrayHelper() {
                override fun get(
                    ctx: Any,
                    index: Int,
                ): Any {
                    return (ctx as ByteArray)[index]
                }

                override fun length(ctx: Any): Int {
                    return (ctx as ByteArray).size
                }
            }
        protected val CHAR_ARRAY_HELPER =
            object : ArrayHelper() {
                override fun get(
                    ctx: Any,
                    index: Int,
                ): Any {
                    return (ctx as CharArray)[index]
                }

                override fun length(ctx: Any): Int {
                    return (ctx as CharArray).size
                }
            }
        protected val SHORT_ARRAY_HELPER =
            object : ArrayHelper() {
                override fun get(
                    ctx: Any,
                    index: Int,
                ): Any {
                    return (ctx as ShortArray)[index]
                }

                override fun length(ctx: Any): Int {
                    return (ctx as ShortArray).size
                }
            }

        protected val INT_ARRAY_HELPER =
            object : ArrayHelper() {
                override fun get(
                    ctx: Any,
                    index: Int,
                ): Any {
                    return (ctx as IntArray)[index]
                }

                override fun length(ctx: Any): Int {
                    return (ctx as IntArray).size
                }
            }

        protected val LONG_ARRAY_HELPER =
            object : ArrayHelper() {
                override fun get(
                    ctx: Any,
                    index: Int,
                ): Any {
                    return (ctx as LongArray)[index]
                }

                override fun length(ctx: Any): Int {
                    return (ctx as LongArray).size
                }
            }

        protected val FLOAT_ARRAY_HELPER =
            object : ArrayHelper() {
                override fun get(
                    ctx: Any,
                    index: Int,
                ): Any {
                    return (ctx as FloatArray)[index]
                }

                override fun length(ctx: Any): Int {
                    return (ctx as FloatArray).size
                }
            }

        protected val DOUBLE_ARRAY_HELPER =
            object : ArrayHelper() {
                override fun get(
                    ctx: Any,
                    index: Int,
                ): Any {
                    return (ctx as DoubleArray)[index]
                }

                override fun length(ctx: Any): Int {
                    return (ctx as DoubleArray).size
                }
            }
    }
}
