package com.ismartcoding.plain.features.call

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression tests for the libphonenumber-backed [PhoneGeoLookup].
 * Locks the Discussion #370 root-cause fix: US trunk-prefixed numbers
 * ("1800...", "1888...") must resolve as US toll-free, never against
 * Chinese carrier/region data. All lookups are offline and deterministic.
 */
class PhoneGeoLookupTest {

    @Test
    fun usTollFreeTrunkPrefixedNumberResolvesAsUsNotChina() {
        val geo = PhoneGeoLookup.lookup("18005551234", "US", Locale.US)!!
        assertEquals("US", geo.country)
        assertEquals("TOLL_FREE", geo.numberType)
        assertTrue(geo.carrier.isEmpty())
    }

    @Test
    fun usTollFreePlusPrefixedNumberResolvesAsUs() {
        val geo = PhoneGeoLookup.lookup("+18005551234", "US", Locale.US)!!
        assertEquals("US", geo.country)
        assertEquals("TOLL_FREE", geo.numberType)
    }

    @Test
    fun cnMobileWithCnRegionResolvesAsCnMobile() {
        val geo = PhoneGeoLookup.lookup("18012345678", "CN", Locale.CHINA)!!
        assertEquals("CN", geo.country)
        assertEquals("MOBILE", geo.numberType)
        assertTrue(geo.carrier.isNotEmpty())
    }

    @Test
    fun sameDigitsWithUsRegionNeverResolveAsChina() {
        val geo = PhoneGeoLookup.lookup("18012345678", "US", Locale.US)
        assertNotEquals("CN", geo?.country)
    }

    @Test
    fun invalidOrUnparseableNumbersReturnNull() {
        assertNull(PhoneGeoLookup.lookup("12345", "US", Locale.US))
        assertNull(PhoneGeoLookup.lookup("not-a-number", "US", Locale.US))
        assertNull(PhoneGeoLookup.lookup("", "US", Locale.US))
    }
}
