package com.ismartcoding.plain.features.fake

import android.provider.Telephony
import com.ismartcoding.lib.helpers.AssetsHelper
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.features.sms.DMessage
import com.ismartcoding.plain.helpers.QueryHelper
import kotlinx.datetime.Instant
import kotlin.random.Random

object SmsFakeStoreHelper {
    private val demoItems = mutableListOf<DMessage>()

    suspend fun count(
        query: String,
    ): Int {
        if (demoItems.isEmpty()) {
            demoSearch()
        }
        if (query.isNotEmpty()) {
            val queryGroups = QueryHelper.parseAsync(query)
            val t = queryGroups.find { it.name == "type" }
            if (t != null) {
                return demoItems.count { it.type == t.value.toInt() }
            }
        }
        return demoItems.size
    }

    suspend fun search(
        query: String,
    ): List<DMessage> {
        if (demoItems.isEmpty()) {
            demoSearch()
        }
        if (query.isNotEmpty()) {
            val queryGroups = QueryHelper.parseAsync(query)
            val t = queryGroups.find { it.name == "type" }
            if (t != null) {
                return demoItems.filter { it.type == t.value.toInt() }
            }
        }
        return demoItems
    }

    private fun demoSearch(): List<DMessage> {
        val json = AssetsHelper.read(MainApp.instance, "sms.json")
        val messages = jsonDecode<List<String>>(json)
        messages.shuffled().forEachIndexed { index, s ->
            demoItems.add(
                DMessage(
                    (index + 4658).toString(),
                    s,
                    Random.nextLong(1234567890, 9234567890).toString(),
                    Instant.fromEpochMilliseconds(System.currentTimeMillis() - Random.nextInt(0, 100 * 3600) * 1000),
                    "",
                    true,
                    "",
                    if (Random.nextInt() % 2 == 0) Telephony.Sms.MESSAGE_TYPE_INBOX else Telephony.Sms.MESSAGE_TYPE_SENT,
                ),
            )
        }

        return demoItems
    }
}
