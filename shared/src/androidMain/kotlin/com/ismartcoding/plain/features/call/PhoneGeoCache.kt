package com.ismartcoding.plain.features.call

import androidx.collection.LruCache
import com.ismartcoding.plain.httpserver.models.PhoneGeo

/**
 * LRU cache for phone-number -> PhoneGeo lookups.
 *
 * libphonenumber parsing + geocoder/carrier metadata lookups cost more than
 * a map hit, and a 200-row calls page repeats the same numbers heavily, so
 * caching by raw number string — including misses for short or unknown
 * numbers — collapses the cost to one lookup per unique number.
 *
 * `LruCache` rejects nullable value types, so a single shared sentinel
 * stands in for the "not found" case.
 */
object PhoneGeoCache {
    private const val MAX_SIZE = 1024
    private val MISS = PhoneGeo("", "", "", "")
    private val cache = object : LruCache<String, PhoneGeo>(MAX_SIZE) {
        override fun sizeOf(key: String, value: PhoneGeo): Int = 1
    }

    fun lookup(number: String): PhoneGeo? {
        cache[number]?.let { return it.takeIf { it !== MISS } }
        val geo = PhoneGeoLookup.lookup(number)
        cache.put(number, geo ?: MISS)
        return geo
    }
}
