package com.ismartcoding.plain.data

import com.ismartcoding.plain.InitQuery
import com.ismartcoding.plain.NetworkQuery
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.VocabulariesQuery
import com.ismartcoding.plain.data.enums.ConfigType
import com.ismartcoding.plain.db.DBox
import com.ismartcoding.plain.extensions.toRoute
import com.ismartcoding.plain.extensions.toRule
import com.ismartcoding.plain.features.DExchangeRates
import com.ismartcoding.plain.features.route.Route
import com.ismartcoding.plain.features.route.getMessage
import com.ismartcoding.plain.features.route.getTitle
import com.ismartcoding.plain.features.rule.Rule
import com.ismartcoding.plain.features.wireguard.WireGuard
import com.ismartcoding.plain.features.wireguard.toWireGuard
import com.ismartcoding.plain.fragment.*
import kotlinx.serialization.json.Json

class UIDataCache {
    var box: DBox? = null
    var devices: MutableList<DeviceFragment>? = null
    var configs: MutableList<ConfigFragment>? = null
    var rules: MutableList<Rule>? = null
    var routes: MutableList<Route>? = null
    var boxNetworkReachable: Boolean? = null
    var boxBluetoothReachable: Boolean? = null
    var network: NetworkInfoFragment? = null
    var networks: MutableList<NetworkFragment>? = null
    var interfaces: MutableList<InterfaceFragment>? = null
    var networkConfig: NetworkConfigFragment? = null
    var latestExchangeRates: DExchangeRates? = null
    var vocabularies: List<VocabulariesQuery.Vocabulary>? = null
    var hostapd: HostapdFragment? = null
    var wireGuards: MutableList<WireGuard>? = null
    var bluetooth: InitQuery.Bluetooth? = null
    var systemConfig = SystemConfig()

    fun clearConnectivityState() {
        boxNetworkReachable = null
        boxBluetoothReachable = null
    }

    fun initNetwork(data: NetworkQuery.Data) {
        devices = data.devices.map { it.deviceFragment }.toMutableList()
        networks = data.networks.map { it.networkFragment }.toMutableList()
        interfaces = data.interfaces.map { it.interfaceFragment }.toMutableList()
        configs = data.configs.map { it.configFragment }.toMutableList()
        rules = configs?.filter { it.group == ConfigType.RULE.value }?.map { it.toRule() }?.toMutableList()
        routes = configs?.filter { it.group == ConfigType.ROUTE.value }?.map { it.toRoute() }?.toMutableList()
        val json =
            Json {
                ignoreUnknownKeys = true
            }
        configs?.find { it.group == ConfigType.SYSTEM.value }?.let {
            systemConfig = json.decodeFromString(it.value)
        }
        wireGuards = data.wireGuards.map { it.wireGuardFragment.toWireGuard() }.toMutableList()
    }

    fun initData(data: InitQuery.Data) {
        network = data.network.networkInfoFragment
        bluetooth = data.bluetooth
    }

    fun getDevices(q: String = ""): List<DeviceFragment> {
        return (
            if (q.isNotEmpty()) {
                devices?.filter { d ->
                    d.name?.contains(q, true) == true || d.ip4.contains(q)
                }
            } else {
                devices
            }
        ) ?: arrayListOf()
    }

    fun getRules(q: String = ""): List<Rule> {
        return (
            if (q.isNotEmpty()) {
                rules?.filter { d ->
                    d.target.contains(q, true) || d.applyTo.contains(q, true)
                }
            } else {
                rules
            }
        ) ?: arrayListOf()
    }

    fun getRoutes(q: String = ""): List<Route> {
        return (
            if (q.isNotEmpty()) {
                routes?.filter { d ->
                    d.getMessage().contains(q, true) || d.getTitle().contains(q, true)
                }
            } else {
                routes
            }
        ) ?: arrayListOf()
    }

    fun getInterfaces(q: String = ""): List<InterfaceFragment> {
        if (interfaces == null) {
            return arrayListOf()
        }
        return if (q.isNotEmpty()) {
            interfaces!!.filter {
                it.name.contains(q, true)
            }
        } else {
            interfaces!!
        }
    }

    fun getNetworks(q: String = ""): List<NetworkFragment> {
        if (networks == null) {
            return arrayListOf()
        }
        return if (q.isNotEmpty()) {
            networks!!.filter {
                it.name.contains(q, true)
            }
        } else {
            networks!!
        }
    }

    fun getSelectableNetworks(q: String = ""): List<NetworkFragment> {
        return getNetworks(q).filter { it.type != "wan" }
    }

    companion object {
        private val cacheMap = mutableMapOf<String, UIDataCache>()

        fun current(): UIDataCache {
            return get(TempData.selectedBoxId)
        }

        fun get(boxId: String): UIDataCache {
            var cache = cacheMap[boxId]
            if (cache == null) {
                cache = UIDataCache()
                cacheMap[boxId] = cache
            }
            return cache
        }
    }
}
