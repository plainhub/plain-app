package com.ismartcoding.plain.features.box

import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.BoxDao
import com.ismartcoding.plain.db.DBox
import com.ismartcoding.plain.fragment.InterfaceFragment
import kotlinx.datetime.Clock

object BoxHelper {
    private val boxDao: BoxDao by lazy {
        AppDatabase.instance.boxDao()
    }

    fun getItemsAsync(): List<DBox> {
        return boxDao.getAll()
    }

    fun addOrUpdateAsync(
        id: String,
        updateItem: (DBox) -> Unit,
    ) {
        var item = boxDao.getById(id)
        var isInsert = false
        if (item == null) {
            item = DBox(id)
            isInsert = true
        } else {
            item.updatedAt = Clock.System.now()
        }

        updateItem(item)

        if (isInsert) {
            boxDao.insert(item)
        } else {
            BoxApi.disposeApolloClients(id)
            boxDao.update(item)
        }
    }

    fun getIPs(interfaces: List<InterfaceFragment>): List<String> {
        val ips = arrayListOf<String>()
        interfaces.forEach {
            if (it.ip4.isNotEmpty() && NetworkHelper.isSiteLocalIP4(it.ip4.split("/")[0])) {
                ips.add(it.ip4)
            }
        }
        return ips
    }

    fun unpairAsync(item: DBox) {
        boxDao.delete(item)
        TempData.selectedBoxId = ""
    }

    fun getSelectedBoxAsync(): DBox? {
        return boxDao.getById(TempData.selectedBoxId)
    }
}
