package com.ismartcoding.plain.db

import androidx.room.*
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import kotlinx.datetime.*
import java.util.*

@Entity(tableName = "boxes")
data class DBox(
    @PrimaryKey var id: String,
) : DEntityBase() {
    @ColumnInfo(name = "name")
    var name: String = ""

    @ColumnInfo(name = "token")
    var token: String = ""

    @ColumnInfo(name = "bluetooth_mac")
    var bluetoothMac: String = ""

    @ColumnInfo(name = "ips")
    var ips: ArrayList<String> = arrayListOf()

    fun getBoxIP(): String {
        val deviceIP = NetworkHelper.getDeviceIP4()
        if (deviceIP.isNotEmpty()) {
            ips.forEach { net ->
                if (NetworkHelper.subnetContains(net, deviceIP)) {
                    return net.split("/")[0]
                }
            }
        }

        val interfaces = NetworkHelper.getNetworkInterfaces()
        for (intf in interfaces.filter { it.name.startsWith("tun") && it.isUp }) {
            val inetAddresses = Collections.list(intf.inetAddresses)
            for (address in inetAddresses) {
                LogCat.d("${intf.name}: " + address.hostAddress)
                ips.forEach { net ->
                    if (NetworkHelper.subnetContains(net, address.hostAddress ?: "")) {
                        return net.split("/")[0]
                    }
                }
            }
        }

        return ""
    }
}

@Dao
interface BoxDao {
    @Query("SELECT * FROM boxes")
    fun getAll(): List<DBox>

    @Query("SELECT * FROM boxes WHERE id=:id")
    fun getById(id: String): DBox?

    @Insert
    fun insert(vararg item: DBox)

    @Update
    fun update(vararg item: DBox)

    @Delete
    fun delete(item: DBox)
}
