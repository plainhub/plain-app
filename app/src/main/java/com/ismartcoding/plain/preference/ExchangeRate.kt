package com.ismartcoding.plain.preference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class ExchangeConfig(
    var base: String = "USD",
    var value: Double = 100.0,
    val selected: MutableSet<String> = mutableSetOf("USD", "CNY", "EUR"),
)

val LocalExchangeRate = compositionLocalOf { ExchangeConfig() }

@Composable
fun ExchangeRateProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val config = ExchangeConfig()
    val state =
        remember {
            context.dataStore.data.map {
                ExchangeRatePreference.getConfig(it)
            }
        }.collectAsStateValue(
            initial = config,
        )

    CompositionLocalProvider(
        LocalExchangeRate provides state,
    ) {
        content()
    }
}
