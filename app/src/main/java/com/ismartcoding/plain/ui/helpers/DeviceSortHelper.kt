package com.ismartcoding.plain.ui.helpers

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.preference.DeviceSortByPreference
import com.ismartcoding.plain.features.device.DeviceSortBy

object DeviceSortHelper {
    suspend fun getSelectedSortItemAsync(
        context: Context,
        menu: Menu,
    ): MenuItem {
        return when (withIO { DeviceSortByPreference.getValueAsync(context) }) {
            DeviceSortBy.NAME_ASC -> {
                menu.findItem(R.id.sort_name_asc)
            }

            DeviceSortBy.NAME_DESC -> {
                menu.findItem(R.id.sort_name_desc)
            }

            DeviceSortBy.IP_ADDRESS -> {
                menu.findItem(R.id.sort_ip_address)
            }

            DeviceSortBy.LAST_ACTIVE -> {
                menu.findItem(R.id.sort_last_active_desc)
            }
        }
    }
}
