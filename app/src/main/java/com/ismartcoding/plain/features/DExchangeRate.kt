package com.ismartcoding.plain.features

import kotlinx.datetime.Instant
import org.json.JSONObject

data class DExchangeRate(
    val currency: String = "",
    val rate: Double = 0.0,
)

class DExchangeRates {
    lateinit var date: Instant
    val rates = mutableListOf<DExchangeRate>()

    fun getBaseRate(base: String): Double {
        return rates.find { it.currency == base }?.rate ?: 1.0
    }

    fun fromJSON(json: JSONObject) {
        date = Instant.fromEpochSeconds(json.optLong("ts"))
        val ratesJSON = json.getJSONObject("quotes")
          for (key in ratesJSON.keys()) {
            if (ignore.contains(key)) {
                continue
            }
            rates.add(DExchangeRate(key, ratesJSON.optDouble(key)))
        }
        rates.add(DExchangeRate(json.optString("base"), 1.0))
    }

    companion object {
        private val ignore =
            setOf("ANG", "ALL", "AFN", "AWG", "AZN", "BAM", "BBD", "BHD", "BIF", "BMD", "BOB", "BSD", "BTC", "BTN", "BYN", "BZD", "CDF", "CLF", "CNH", "COP", "CUC", "CUP", "CVE", "DJF", "DOP", "DZD", "ERN", "ETB", "FKP", "GIP", "GMD", "GNF", "GTQ", "GYD", "HNL", "HTG", "IQD", "IRR", "JMD", "JOD", "KGS", "KHR", "KMF", "KPW", "KWD", "KYD", "KZT", "LBP", "LRD", "LYD", "MDL", "MGA", "MKD", "MMK", "MNT", "MOP", "MRU", "MUR", "MVR", "MWK", "MZN", "NAD", "NIO", "OMR", "PGK", "PYG", "QAR", "RSD", "RWF", "SAR", "SBD", "SCR", "SDG", "SHP", "SLL", "SOS", "SRD", "SSP", "STD", "STN", "SVC", "SYP", "SZL", "TJS", "TND", "TOP", "TTD", "TWD", "UYU", "UZS", "VES", "VUV", "WST", "XAF", "XAG", "XAU", "XCD", "XDR", "XPD", "XPF", "XPT", "YER", "ZMW", "ZWL")
    }
}
