package com.ismartcoding.plain.discover

import com.ismartcoding.plain.preferences.UserPrefs
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ble.client.BleGattClient
import com.ismartcoding.plain.ble.server.BlePairingSessionStore
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.data.DPairingResult
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.enums.DiscoveryMethod
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PairingCanceledEvent
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.getAppVersion
import com.ismartcoding.plain.platform.getDeviceIP4s
import com.ismartcoding.plain.platform.getDeviceName
import com.ismartcoding.plain.platform.getDeviceType
import com.ismartcoding.plain.platform.getPlatformName
import com.ismartcoding.plain.platform.isWifiAwareSupported
import com.ismartcoding.plain.ui.models.NearbyViewModel

object PairingCore {

    // ---- Discovery ----------------------------------------------------------

    fun buildDiscoverReply(): DDiscoverReply {
        return DDiscoverReply(
            id = TempData.clientId,
            name = TempData.deviceName.value.ifEmpty { getDeviceName() },
            deviceType = getDeviceType(),
            port = UserPrefs.httpsPort.value,
            version = getAppVersion(),
            platform = getPlatformName(),
            ips = getDeviceIP4s(),
            awareSupported = isWifiAwareSupported,
            awareRunning = TempData.awareRunning.value,
        )
    }

    fun formatMessage(type: NearbyMessageType, json: String): String {
        return "${type.toPrefix()}$json"
    }

    fun replyToDevice(reply: DDiscoverReply, bleClient: BleGattClient? = null): DNearbyDevice {
        return DNearbyDevice(
            id = reply.id,
            name = reply.name,
            ips = reply.ips,
            port = reply.port,
            deviceType = reply.deviceType,
            version = reply.version,
            platform = reply.platform,
            lastSeen = TimeHelper.now(),
            discoveryMethods = if (bleClient == null) setOf(DiscoveryMethod.LAN) else setOf(DiscoveryMethod.BLE),
            bleClient = bleClient,
        )
    }

    // ---- Initiator (send request / receive response) -----------------------

    suspend fun startPairingSession(device: DNearbyDevice, deviceIp: String): Pair<DPairingRequest, com.ismartcoding.plain.data.DPairingTicket> =
        RustPairingStore.start(device.id, device.name, deviceIp, device.port)

    suspend fun handlePairResponse(response: DPairingResponse, senderIp: String): Boolean? {
        val outcome = RustPairingStore.complete(response, senderIp) ?: return null
        val peer = outcome.peer
        if (peer == null) {
            notifyFailed(outcome.ticket.deviceId, outcome.ticket.deviceName, outcome.error)
            return false
        }
        publishSuccess(peer.id, peer.name, senderIp, peer.key)
        return true
    }

    suspend fun handlePairRequest(request: DPairingRequest, senderAddress: String, isBle: Boolean) {
        if (!isBle) request.fromIp = senderAddress
        val new = RustPairingStore.receiveRequest(request) ?: return
        if (isBle) BlePairingSessionStore.put(request.fromId, senderAddress)
        if (!new) return
        sendEvent(PairingRequestReceivedEvent(request))
        sendEvent(WebSocketEvent(EventType.PAIRING_REQUEST_RECEIVED, JsonHelper.jsonEncode(request)))
    }

    suspend fun buildRejectionResponse(request: DPairingRequest): DPairingResponse? = RustPairingStore.respond(request, false)?.first

    suspend fun handlePairCancel(cancel: DPairingCancel) {
        val ticket = RustPairingStore.receiveCancel(cancel) ?: return
        sendEvent(PairingCanceledEvent(ticket.deviceId))
        sendEvent(WebSocketEvent(EventType.PAIRING_CANCELED, JsonHelper.jsonEncode(DPairingResult(deviceId = ticket.deviceId, deviceName = ticket.deviceName))))
    }

    suspend fun acceptPairingRequest(request: DPairingRequest): DPairingResponse? {
        val built = RustPairingStore.respond(request, true) ?: return null
        val peer = requireNotNull(built.second)
        publishSuccess(peer.id, peer.name, request.fromIp, peer.key)
        return built.first
    }

    private suspend fun publishSuccess(id: String, name: String, ip: String, key: String) {
        com.ismartcoding.plain.chat.peer.PeerManager.load()
        NearbyViewModel.handlePairingSuccess(id)
        sendEvent(PairingSuccessEvent(id, name, ip, key))
        sendEvent(WebSocketEvent(EventType.PAIRING_SUCCESS, JsonHelper.jsonEncode(DPairingResult(deviceId = id, deviceName = name))))
    }

    fun notifyFailed(deviceId: String, deviceName: String, reason: String) {
        NearbyViewModel.itemStatus.remove(deviceId)
        sendEvent(
            WebSocketEvent(
                EventType.PAIRING_FAILED,
                JsonHelper.jsonEncode(DPairingResult(deviceId = deviceId, deviceName = deviceName, error = reason)),
            )
        )
    }

}
