package com.ismartcoding.plain.platform

import android.content.Context
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper
import com.google.i18n.phonenumbers.Phonenumber
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

internal actual fun phoneMetadata(request: PhoneMetadataRequest): PhoneMetadataFacts {
    val leadingZeros = request.nationalNumber.takeWhile { it == '0' }.length
    val number = Phonenumber.PhoneNumber().setCountryCode(request.countryCode)
        .setNationalNumber(request.nationalNumber.toLong())
    if (leadingZeros > 0) number.setItalianLeadingZero(true).setNumberOfLeadingZeros(leadingZeros)
    val locale = Locale.forLanguageTag(request.locale)
    return PhoneMetadataFacts(
        if (request.includeCarrier) PhoneNumberToCarrierMapper.getInstance().getNameForValidNumber(number, locale).orEmpty() else "",
        PhoneNumberOfflineGeocoder.getInstance().getDescriptionForValidNumber(number, locale).orEmpty(),
    )
}
