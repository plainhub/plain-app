package com.ismartcoding.plain.features.exchange

import kotlinx.serialization.Serializable

@Serializable
class ExchangeConfig(var base: String = "USD", var value: Double = 100.0, val selected: MutableSet<String> = mutableSetOf("USD", "CNY", "EUR"))