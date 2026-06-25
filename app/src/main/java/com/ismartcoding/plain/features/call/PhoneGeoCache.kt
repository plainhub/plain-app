package com.ismartcoding.plain.features.call

import androidx.collection.LruCache
import com.ismartcoding.plain.lib.phonegeo.PhoneNumberLookup
import com.ismartcoding.plain.lib.phonegeo.algo.LookupAlgorithm
import com.ismartcoding.plain.web.models.PhoneGeo

/**
 * LRU cache for phone-number -> PhoneGeo lookups.
 *
 * `PhoneNumberLookup` does an in-memory binary search on a 4MB phone.dat
 * (avg ~19 comparisons, 1-5ms per call). For a 200-row calls page the
 * same numbers repeat heavily, so caching by raw number string —
 * including misses for short or unknown numbers — collapses that cost
 * to one lookup per unique number.
 *
 * `LruCache` rejects nullable value types, so a single shared sentinel
 * stands in for the "not found" case.
 */
object PhoneGeoCache {
    private const val MAX_SIZE = 1024
    private val MISS: PhoneGeo = PhoneGeo("", "", "", "", 0)
    private val cache = object : LruCache<String, PhoneGeo>(MAX_SIZE) {
        override fun sizeOf(key: String, value: PhoneGeo): Int = 1
    }

    fun lookup(number: String): PhoneGeo? {
        cache[number]?.let { return it.takeIf { it !== MISS } }
        val geo = lookupFresh(number)
        cache.put(number, geo ?: MISS)
        return geo
    }

    private fun lookupFresh(number: String): PhoneGeo? =
        PhoneNumberLookup.instance()
            .with(LookupAlgorithm.IMPL.BINARY_SEARCH)
            .lookup(number)
            ?.let { PhoneGeo(it.geoInfo.province, it.geoInfo.city, it.geoInfo.zipCode, it.geoInfo.areaCode, it.isp.code) }
}
