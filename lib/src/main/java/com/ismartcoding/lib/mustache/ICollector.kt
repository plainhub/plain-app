package com.ismartcoding.lib.mustache

import com.ismartcoding.lib.mustache.Mustache.VariableFetcher

/** Handles interpreting objects as collections.  */
interface ICollector {
    /** Returns an iterator that can iterate over the supplied value, or null if the value is
     * not a collection.  */
    fun toIterator(value: Any): Iterator<*>?

    /** Creates a fetcher for a so-named variable in the supplied context object, which will
     * never be null. The fetcher will be cached and reused for future contexts for which
     * `octx.getClass().equals(nctx.getClass()`.  */
    fun createFetcher(
        ctx: Any,
        name: String,
    ): VariableFetcher?

    /** Creates a map to be used to cache [Mustache.VariableFetcher] instances. The GWT-compatible
     * collector returns a HashMap here, but the reflection based fetcher (which only works on
     * the JVM and Android, returns a concurrent hashmap.  */
    fun <K, V> createFetcherCache(): MutableMap<K, V>
}
