package com.ismartcoding.plain.features.call

import android.annotation.SuppressLint
import android.net.Uri
import com.ismartcoding.lib.extensions.telecomManager
import com.ismartcoding.plain.MainApp

@SuppressLint("MissingPermission")
object SimHelper {
    fun getAll(): List<DSim> {
        val context = MainApp.instance
        val accounts = mutableListOf<DSim>()
        context.telecomManager.callCapablePhoneAccounts.forEach { account ->
            val phoneAccount = context.telecomManager.getPhoneAccount(account)
            val label = phoneAccount.label.toString()
            var address = phoneAccount.address.toString()
            if (address.startsWith("tel:") && address.substringAfter("tel:").isNotEmpty()) {
                address = Uri.decode(address.substringAfter("tel:"))
            }
            accounts.add(DSim(phoneAccount.accountHandle.id, label, address))
        }
        return accounts
    }

    fun hasMultiSims(): Boolean {
        return MainApp.instance.telecomManager.callCapablePhoneAccounts.size > 1
    }
}
