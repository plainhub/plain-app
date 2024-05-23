package com.ismartcoding.plain.ui.call

import android.provider.CallLog
import com.ismartcoding.lib.extensions.formatDuration
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DCall
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.models.IDataModel
import com.ismartcoding.plain.ui.models.ListItemModel

data class CallModel(override val data: DCall) : IDataModel, ListItemModel() {
    fun getDurationText(): String {
        return when (data.type) {
            CallLog.Calls.MISSED_TYPE -> {
                LocaleHelper.getString(R.string.call_missed)
            }
            CallLog.Calls.INCOMING_TYPE -> {
                LocaleHelper.getString(R.string.call_incoming) + " " + data.duration.toLong().formatDuration()
            }
            CallLog.Calls.OUTGOING_TYPE -> {
                if (data.duration == 0) {
                    LocaleHelper.getString(R.string.call_not_connected)
                } else {
                    LocaleHelper.getString(R.string.call_outgoing) + " " + data.duration.toLong().formatDuration()
                }
            }
            else -> ""
        }
    }

    fun getGeoText(): String {
        val geo = data.getGeo() ?: return ""

        val texts = mutableListOf<String>()
        if (geo.isp > 0) {
            texts.add(getString("phone_isp_type${geo.isp}"))
        }

        if (geo.city == geo.province) {
            texts.add(geo.city)
        } else {
            texts.add("${geo.province}${geo.city}")
        }

        return texts.joinToString(", ")
    }
}
