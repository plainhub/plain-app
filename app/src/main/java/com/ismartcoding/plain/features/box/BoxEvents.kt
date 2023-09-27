package com.ismartcoding.plain.features.box

import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.*
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.api.GraphqlApiResult
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.features.wireguard.toWireGuard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.MutableMap
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toMutableList
import kotlin.system.measureTimeMillis

interface IBoxEvent {
    val boxId: String
}

data class FetchInitDataEvent(override val boxId: String) : IBoxEvent {
    companion object {
        fun createDefault(): FetchInitDataEvent {
            return FetchInitDataEvent(TempData.selectedBoxId)
        }
    }
}

data class FetchNetworkConfigEvent(override val boxId: String) : IBoxEvent

data class FetchNetworksEvent(override val boxId: String) : IBoxEvent

data class FetchWireGuardsEvent(override val boxId: String) : IBoxEvent

data class FetchVocabulariesEvent(override val boxId: String) : IBoxEvent

data class NetworkConfigResultEvent(val boxId: String, val result: GraphqlApiResult<NetworkConfigQuery.Data>)

data class ApplyHostapdResultEvent(val boxId: String, val result: GraphqlApiResult<ApplyHostapdMutation.Data>)

data class ApplyWireGuardResultEvent(val boxId: String, val result: GraphqlApiResult<ApplyWireGuardMutation.Data>)

data class WireGuardsResultEvent(val boxId: String, val result: GraphqlApiResult<WireGuardsQuery.Data>)

data class NetworksResultEvent(val boxId: String, val result: GraphqlApiResult<NetworkQuery.Data>)

data class CancelActiveJobEvent(val boxId: String, val eventClass: Class<Any>)

data class InitDataResultEvent(val boxId: String, val result: GraphqlApiResult<InitQuery.Data>)

data class VocabulariesResultEvent(val boxId: String, val result: GraphqlApiResult<VocabulariesQuery.Data>)

object BoxEvents {
    private val eventJobs: MutableMap<String, Job> = mutableMapOf()

    private fun getJobKey(
        boxId: String,
        eventClass: Class<Any>,
    ): String {
        return "$boxId:${eventClass.simpleName}"
    }

    private fun safeRun(
        event: IBoxEvent,
        runJob: () -> Job,
    ) {
        if (eventJobs[getJobKey(event.boxId, event.javaClass)]?.isActive == true) {
            return
        }
        eventJobs[getJobKey(event.boxId, event.javaClass)] = runJob()
    }

    fun register() {
        receiveEventHandler<FetchInitDataEvent> { event ->
            safeRun(event) {
                launch {
                    val r: GraphqlApiResult<InitQuery.Data>
                    val t =
                        measureTimeMillis {
                            r =
                                withIO {
                                    BoxApi.mixQueryAsync(InitQuery())
                                }
                            if (r.isSuccess()) {
                                UIDataCache.get(TempData.selectedBoxId).initData(r.response?.data!!)
                            }
                        }
                    if (t < 500) {
                        delay(500 - t) // to make sure the loading text of top bar is not refreshed so fast.
                    }
                    sendEvent(InitDataResultEvent(event.boxId, r))
                }
            }
        }

        receiveEventHandler<FetchNetworkConfigEvent> { event ->
            safeRun(event) {
                launch {
                    val r = withIO { BoxApi.mixQueryAsync(NetworkConfigQuery()) }
                    if (r.isSuccess()) {
                        r.response?.data?.let { data ->
                            UIDataCache.current().run {
                                networkConfig = data.networkConfig.networkConfigFragment
                                hostapd = data.hostapd.hostapdFragment
                            }
                        }
                    }
                    sendEvent(NetworkConfigResultEvent(event.boxId, r))
                }
            }
        }

        receiveEventHandler<FetchNetworksEvent> { event ->
            safeRun(event) {
                launch {
                    val r = withIO { BoxApi.mixQueryAsync(NetworkQuery()) }
                    if (r.isSuccess()) {
                        r.response?.data?.let { data ->
                            UIDataCache.current().initNetwork(data)
                        }
                    }
                    sendEvent(NetworksResultEvent(event.boxId, r))
                }
            }
        }

        receiveEventHandler<FetchWireGuardsEvent> { event ->
            safeRun(event) {
                launch {
                    val r = withIO { BoxApi.mixQueryAsync(WireGuardsQuery()) }
                    if (r.isSuccess()) {
                        r.response?.data?.let { data ->
                            UIDataCache.current().run {
                                wireGuards = data.wireGuards.map { it.wireGuardFragment.toWireGuard() }.toMutableList()
                            }
                        }
                    }
                    sendEvent(WireGuardsResultEvent(event.boxId, r))
                }
            }
        }

        receiveEventHandler<FetchVocabulariesEvent> { event ->
            safeRun(event) {
                launch {
                    val r = withIO { BoxApi.mixQueryAsync(VocabulariesQuery()) }
                    if (r.isSuccess()) {
                        r.response?.data?.let { data ->
                            UIDataCache.current().run {
                                vocabularies = data.vocabularies
                            }
                        }
                    }
                    sendEvent(VocabulariesResultEvent(event.boxId, r))
                }
            }
        }

        receiveEventHandler<CancelActiveJobEvent> { event ->
            eventJobs[getJobKey(event.boxId, event.eventClass)]?.cancel()
        }
    }
}
