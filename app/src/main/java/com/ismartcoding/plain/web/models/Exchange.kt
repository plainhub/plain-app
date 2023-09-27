package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.DExchangeRates

data class ExchangeRate(
    val k: String = "",
    val v: Double = 0.0,
)

data class ExchangeRates(
    val date: String = "",
    val rates: List<ExchangeRate>,
)

fun DExchangeRates.toModel(): ExchangeRates {
    return ExchangeRates(date, rates.map { ExchangeRate(it.currency, it.rate) })
}
