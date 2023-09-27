package com.ismartcoding.plain.features.device

import android.content.Context
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.ISelectOption
import com.ismartcoding.plain.data.preference.DeviceSortByPreference
import com.ismartcoding.plain.features.locale.LocaleHelper.getString

enum class DeviceSortBy : ISelectOption {
    NAME_ASC,
    NAME_DESC,
    IP_ADDRESS,
    LAST_ACTIVE,
    ;

    override fun getText(): String {
        return when (this) {
            NAME_ASC -> getString(R.string.name_asc)
            NAME_DESC -> getString(R.string.name_desc)
            IP_ADDRESS -> getString(R.string.ip_address_asc)
            LAST_ACTIVE -> getString(R.string.last_active_desc)
        }
    }

    override suspend fun isSelected(context: Context): Boolean {
        return DeviceSortByPreference.getValueAsync(context) == this
    }
}
