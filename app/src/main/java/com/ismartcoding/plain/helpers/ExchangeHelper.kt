package com.ismartcoding.plain.helpers

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.features.DExchangeRates
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.json.JSONObject

object ExchangeHelper {
    suspend fun getRates(): DExchangeRates? {
        val client = HttpClientManager.httpClient()
        try {
            val r = client.get("https://raw.githubusercontent.com/ismartcoding/currency-api/main/latest/data.json")
            if (r.status == HttpStatusCode.OK) {
                val json = r.body<String>()
                val rates = DExchangeRates()
                UIDataCache.current().run {
                    rates.fromJSON(JSONObject(json))
                    latestExchangeRates = rates
                }
                return rates
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }

        return null
    }
}
