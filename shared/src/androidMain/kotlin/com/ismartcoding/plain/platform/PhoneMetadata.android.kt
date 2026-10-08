package com.ismartcoding.plain.platform

import android.content.Context
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper
import com.google.i18n.phonenumbers.Phonenumber
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import com.ismartcoding.plain.appContextOrNull
import com.ismartcoding.plain.features.call.PhoneLocaleFacts
import com.ismartcoding.plain.features.call.PhoneMetadataFacts
import com.ismartcoding.plain.features.call.PhoneMetadataRequest
import java.util.Locale

internal actual fun phoneLocaleFacts(): PhoneLocaleFacts {
    val telephony = appContextOrNull?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val locale = Locale.getDefault()
    val region = telephony?.networkCountryIso?.takeIf { it.isNotBlank() }
        ?: telephony?.simCountryIso?.takeIf { it.isNotBlank() } ?: locale.country
    return PhoneLocaleFacts(region.uppercase(Locale.US), locale.toLanguageTag(), true)
}

/**
 * Number classification lives here, on Android, because that is where the
 * libphonenumber data for the device's region lives. The server side used to
 * parse the number itself and hand back a country code and national number
 * just so this function could rebuild the very same number object.
 */
internal actual fun phoneMetadata(request: PhoneMetadataRequest): PhoneMetadataFacts {
    val util = PhoneNumberUtil.getInstance()
    val region = request.region.takeIf { it.isNotBlank() }?.uppercase(Locale.US)
    val number = runCatching { util.parse(request.number, region) }.getOrNull()
        ?.takeIf { util.isValidNumber(it) }
        ?: return PhoneMetadataFacts("", "", "", "")
    val numberType = numberTypeName(util.getNumberType(number))
    val country = util.getRegionCodeForCountryCode(number.countryCode).orEmpty()
    val locale = Locale.forLanguageTag(request.locale)
    val carrier = if (numberType in CARRIER_TYPES) {
        PhoneNumberToCarrierMapper.getInstance().getNameForValidNumber(number, locale).orEmpty()
    } else {
        ""
    }
    return PhoneMetadataFacts(
        country,
        numberType,
        carrier,
        PhoneNumberOfflineGeocoder.getInstance().getDescriptionForValidNumber(number, locale).orEmpty(),
    )
}

/** Only these types have a named carrier; asking for the others is noise. */
private val CARRIER_TYPES = setOf("MOBILE", "FIXED_LINE_OR_MOBILE", "PAGER")

private fun numberTypeName(type: PhoneNumberType): String = when (type) {
    PhoneNumberType.FIXED_LINE -> "FIXED_LINE"
    PhoneNumberType.MOBILE -> "MOBILE"
    PhoneNumberType.FIXED_LINE_OR_MOBILE -> "FIXED_LINE_OR_MOBILE"
    PhoneNumberType.TOLL_FREE -> "TOLL_FREE"
    PhoneNumberType.PREMIUM_RATE -> "PREMIUM_RATE"
    PhoneNumberType.SHARED_COST -> "SHARED_COST"
    PhoneNumberType.PERSONAL_NUMBER -> "PERSONAL_NUMBER"
    PhoneNumberType.VOIP -> "VOIP"
    PhoneNumberType.PAGER -> "PAGER"
    PhoneNumberType.UAN -> "UAN"
    PhoneNumberType.VOICEMAIL -> "VOICEMAIL"
    else -> ""
}
