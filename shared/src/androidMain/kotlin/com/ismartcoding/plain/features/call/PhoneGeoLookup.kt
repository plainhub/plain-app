package com.ismartcoding.plain.features.call

import android.content.Context
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import com.ismartcoding.plain.appContextOrNull
import com.ismartcoding.plain.httpserver.models.PhoneGeo
import java.util.Locale

/**
 * Offline phone number metadata via libphonenumber (region, number type,
 * carrier, geo description). Region-aware parsing: ambiguous trunk-prefixed
 * numbers (e.g. US "18005551234" vs CN mobile "18012345678") are interpreted
 * against the device's telephony region, so they can never fall through to
 * a wrong country's data.
 */
object PhoneGeoLookup {
    private val util: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }
    private val carrierMapper: PhoneNumberToCarrierMapper by lazy { PhoneNumberToCarrierMapper.getInstance() }
    private val geocoder: PhoneNumberOfflineGeocoder by lazy { PhoneNumberOfflineGeocoder.getInstance() }

    fun lookup(number: String, region: String = defaultRegion(), locale: Locale = Locale.getDefault()): PhoneGeo? {
        if (number.isBlank()) return null
        val parsed = try {
            util.parse(number, region)
        } catch (_: NumberParseException) {
            return null
        }
        if (!util.isValidNumber(parsed)) return null
        val type = util.getNumberType(parsed)
        return PhoneGeo(
            country = util.getRegionCodeForNumber(parsed).orEmpty(),
            numberType = if (type == PhoneNumberUtil.PhoneNumberType.UNKNOWN) "" else type.name,
            carrier = carrierMapper.getNameForNumber(parsed, locale).orEmpty(),
            description = geocoder.getDescriptionForNumber(parsed, locale).orEmpty(),
        )
    }

    private fun defaultRegion(): String {
        val tm = appContextOrNull?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return (tm?.networkCountryIso?.takeIf { it.isNotBlank() } ?: tm?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().country).uppercase(Locale.US)
    }
}
