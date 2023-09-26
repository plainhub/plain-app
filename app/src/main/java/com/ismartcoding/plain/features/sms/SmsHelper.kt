package com.ismartcoding.plain.features.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.data.enums.SortDirection
import com.ismartcoding.lib.extensions.getIntValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeValue
import com.ismartcoding.lib.extensions.toStringList
import com.ismartcoding.lib.helpers.AssetsHelper
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.features.BaseContentHelper
import kotlinx.datetime.Instant
import org.json.JSONArray
import kotlin.random.Random

object SmsHelper : BaseContentHelper() {
    override val uriExternal: Uri = Telephony.Sms.CONTENT_URI
    override val idKey: String = Telephony.Sms._ID
    private val demoItems = mutableListOf<DMessage>()

    override fun getProjection(): Array<String> {
        return arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.TYPE,
            Telephony.Sms.BODY,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.READ,
            Telephony.Sms.DATE,
            Telephony.Sms.SERVICE_CENTER
        )
    }

    override fun getWhere(query: String): ContentWhere {
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            val queryGroups = SearchHelper.parse(query)
            queryGroups.forEach {
                if (it.name == "text") {
                    where.add("${Telephony.Sms.BODY} LIKE ?", "%${it.value}%")
                } else if (it.name == "ids") {
                    val ids = it.value.split(",")
                    if (ids.isNotEmpty()) {
                        where.addIn(Telephony.Sms._ID, ids)
                    }
                } else if (it.name == "type") {
                    where.add("${Telephony.Sms.TYPE} = ?", it.value)
                }
            }
        }

        return where
    }

    override fun count(context: Context, query: String): Int {
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

    fun search(context: Context, query: String, limit: Int, offset: Int): List<DMessage> {
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

        val cursor = getSearchCursorWithSortOrder(context, query, limit, offset, SortBy(Telephony.Sms.DATE, SortDirection.DESC))
        val items = mutableListOf<DMessage>()
        if (cursor?.moveToFirst() == true) {
            val cache = mutableMapOf<String, Int>()
            do {
                items.add(
                    DMessage(
                        cursor.getStringValue(Telephony.Sms._ID, cache),
                        cursor.getStringValue(Telephony.Sms.BODY, cache),
                        cursor.getStringValue(Telephony.Sms.ADDRESS, cache),
                        cursor.getTimeValue(Telephony.Sms.DATE, cache),
                        cursor.getStringValue(Telephony.Sms.SERVICE_CENTER, cache),
                        cursor.getIntValue(Telephony.Sms.READ, cache) == 1,
                        cursor.getStringValue(Telephony.Sms.THREAD_ID, cache),
                        cursor.getIntValue(Telephony.Sms.TYPE, cache)
                    )
                )
            } while (cursor.moveToNext())
        }

        return items
    }

    private fun demoSearch(): List<DMessage> {
        val json = AssetsHelper.read(MainApp.instance, "sms.json")
        val array = JSONArray(json)
        val messages = array.toStringList()
        messages.shuffled().forEachIndexed { index, s ->
            demoItems.add(
                DMessage(
                    (index + 4658).toString(), s, Random.nextLong(1234567890, 9234567890).toString(),
                    Instant.fromEpochMilliseconds(System.currentTimeMillis() - Random.nextInt(0, 100 * 3600) * 1000),
                    "", true, "", if (Random.nextInt() % 2 == 0) Telephony.Sms.MESSAGE_TYPE_INBOX else Telephony.Sms.MESSAGE_TYPE_SENT,
                )
            )
        }

        return demoItems
    }
}