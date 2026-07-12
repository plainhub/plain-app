package com.ismartcoding.plain.features.call
import com.ismartcoding.plain.appContext

import android.annotation.SuppressLint
import android.os.Build
import android.telephony.SubscriptionManager
import com.ismartcoding.plain.data.DSim
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.subscriptionManager

@SuppressLint("MissingPermission")
object SimHelper {
    fun getAll(): List<DSim> {
        val context = appContext
        if (!Permission.READ_PHONE_STATE.isGranted()) return emptyList()
        val subs = subscriptionManager.activeSubscriptionInfoList ?: return emptyList()
        return subs.map { info ->
            DSim(
                id = info.subscriptionId.toString(),
                label = info.displayName?.toString() ?: info.carrierName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                number = info.number ?: "",
                subscriptionId = info.subscriptionId,
            )
        }
    }

    fun hasMultiSims(): Boolean {
        if (!Permission.READ_PHONE_STATE.isGranted()) return false
        return (subscriptionManager.activeSubscriptionInfoList?.size ?: 0) > 1
    }
}
