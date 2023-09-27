package com.ismartcoding.plain.features.call

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.BaseColumns
import android.provider.CallLog
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.data.enums.SortDirection
import com.ismartcoding.lib.extensions.getIntValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeValue
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.features.BaseContentHelper
import kotlinx.datetime.Instant
import kotlin.random.Random

object CallHelper : BaseContentHelper() {
    override val uriExternal: Uri = CallLog.Calls.CONTENT_URI
    override val idKey: String = CallLog.Calls._ID

    private val demoItems = mutableListOf<DCall>()

    override fun getProjection(): Array<String> {
        return arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
            CallLog.Calls.PHONE_ACCOUNT_ID,
        )
    }

    override fun getWhere(query: String): ContentWhere {
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            val queryGroups = SearchHelper.parse(query)
            queryGroups.forEach {
                if (it.name == "text") {
                    where.add("${CallLog.Calls.NUMBER} LIKE ?", "%${it.value}%")
                } else if (it.name == "ids") {
                    val ids = it.value.split(",")
                    if (ids.isNotEmpty()) {
                        where.addIn(BaseColumns._ID, ids)
                    }
                } else if (it.name == "type") {
                    where.add("${CallLog.Calls.TYPE} = ?", it.value)
                }
            }
        }

        return where
    }

    override fun count(
        context: Context,
        query: String,
    ): Int {
        if (TempData.demoMode) {
            if (demoItems.isEmpty()) {
                demoSearch()
            }
            if (query.isNotEmpty()) {
                val queryGroups = SearchHelper.parse(query)
                val t = queryGroups.find { it.name == "type" }
                if (t != null) {
                    return demoItems.count { it.type == t.value.toInt() }
                }
            }
            return demoItems.size
        }

        return super.count(context, query)
    }

    fun search(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
    ): List<DCall> {
        if (TempData.demoMode) {
            if (demoItems.isEmpty()) {
                demoSearch()
            }
            if (query.isNotEmpty()) {
                val queryGroups = SearchHelper.parse(query)
                val t = queryGroups.find { it.name == "type" }
                if (t != null) {
                    return demoItems.filter { it.type == t.value.toInt() }
                }
            }
            return demoItems
        }

        val cursor = getSearchCursor(context, query, limit, offset, SortBy(CallLog.Calls._ID, SortDirection.DESC))
        val items = mutableListOf<DCall>()
        if (cursor?.moveToFirst() == true) {
            val cache = mutableMapOf<String, Int>()
            do {
                val id = cursor.getStringValue(CallLog.Calls._ID, cache)
                val number = cursor.getStringValue(CallLog.Calls.NUMBER, cache)
                val name = cursor.getStringValue(CallLog.Calls.CACHED_NAME, cache)
                val photoUri = cursor.getStringValue(CallLog.Calls.CACHED_PHOTO_URI, cache)
                val startTS = cursor.getTimeValue(CallLog.Calls.DATE, cache)
                val duration = cursor.getIntValue(CallLog.Calls.DURATION, cache)
                val type = cursor.getIntValue(CallLog.Calls.TYPE, cache)
                val accountId = cursor.getStringValue(CallLog.Calls.PHONE_ACCOUNT_ID, cache)
                items.add(DCall(id, number, name, photoUri, startTS, duration, type, accountId))
            } while (cursor.moveToNext())
        }

        return items
    }

    fun call(
        context: Context,
        number: String,
    ) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$number")
        context.startActivity(callIntent)
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
