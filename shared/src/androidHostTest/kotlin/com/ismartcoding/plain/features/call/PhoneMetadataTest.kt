package com.ismartcoding.plain.features.call

import com.ismartcoding.plain.platform.phoneMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Number classification now lives on the platform side, so this is where it
 * is pinned: the numbers and the expected answers used to live in the Rust
 * test for `phone_geo`, checking the same facts against the real
 * libphonenumber metadata instead of a Rust copy of it.
 */
class PhoneMetadataTest {

    @Test
    fun a_us_toll_free_number_has_no_carrier() {
        val facts = phoneMetadata(PhoneMetadataRequest("18005551234", "US", "en_US"))
        assertEquals("US", facts.country)
        assertEquals("TOLL_FREE", facts.numberType)
        assertEquals("", facts.carrier)
    }

    @Test
    fun a_cn_mobile_number_is_recognised_and_has_a_carrier() {
        val facts = phoneMetadata(PhoneMetadataRequest("18012345678", "CN", "zh_CN"))
        assertEquals("CN", facts.country)
        assertEquals("MOBILE", facts.numberType)
        assertTrue(facts.carrier.isNotEmpty(), "mobile numbers carry a carrier")
    }

    @Test
    fun the_number_may_carry_its_own_country_code() {
        assertEquals(
            "US",
            phoneMetadata(PhoneMetadataRequest("+18005551234", "CN", "en_US")).country,
        )
    }

    @Test
    fun numbers_we_cannot_make_sense_of_read_back_as_nothing() {
        for (number in listOf("12345", "not-a-number", "")) {
            val facts = phoneMetadata(PhoneMetadataRequest(number, "US", "en_US"))
            assertEquals("", facts.country, "number: $number")
            assertEquals("", facts.numberType, "number: $number")
            assertEquals("", facts.carrier, "number: $number")
            assertEquals("", facts.description, "number: $number")
        }
    }

    @Test
    fun a_blank_region_leaves_the_number_unparsed_rather_than_guessing() {
        assertEquals("", phoneMetadata(PhoneMetadataRequest("18012345678", "", "zh_CN")).country)
    }
}
