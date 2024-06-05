package com.ismartcoding.plain.features.fake

import android.provider.CallLog
import com.ismartcoding.plain.data.DCall
import com.ismartcoding.plain.helpers.QueryHelper
import kotlinx.datetime.Instant
import kotlin.random.Random

object CallFakeStoreHelper {
    private val demoItems = mutableListOf<DCall>()
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
        limit: Int,
        offset: Int,
    ): List<DCall> {
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

    private fun demoSearch(): List<DCall> {
        IntRange(1, 385).forEachIndexed { index, _ ->
            demoItems.add(
                DCall(
                    (index + 658).toString(),
                    Random.nextLong(1234567890, 9234567890).toString(),
                    "",
                    "",
                    Instant.fromEpochMilliseconds(System.currentTimeMillis() - Random.nextInt(0, 100 * 3600) * 1000),
                    Random.nextInt(200),
                    if (Random.nextInt() % 2 == 0) CallLog.Calls.INCOMING_TYPE else CallLog.Calls.OUTGOING_TYPE,
                    "",
                ),
            )
        }

        return demoItems
    }
}