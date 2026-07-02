package com.ismartcoding.plain.features.bluetooth

import android.util.Base64
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.discover.PairingSecurity
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.lib.helpers.NetworkHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.security.KeyPair
import java.util.UUID

object PairingTransport {
    private var server: PairingGattServer? = null
    private var localKeyPair: KeyPair? = null
    private var localRequest: DPairingRequest? = null

    suspend fun startAdvertising() {
        if (server != null) return
        val kp = CryptoHelper.generateECDHKeyPair()
        localKeyPair = kp
        val req = buildLocalRequest(kp)
        localRequest = req
        val s = PairingGattServer(
            requestProvider = { localRequest ?: req },
            onPeerRequest = { handlePeerRequest(it) },
        )
        s.start()
        server = s
    }

    fun stopAdvertising() {
        server?.stop()
        server = null
        localKeyPair = null
        localRequest = null
    }

    fun scanForPairingCandidates(): Flow<BlePairingCandidate> = flow {
        BluetoothUtil.scan(PairingGattServer.SERVICE_UUID).collect { btDevice ->
            emit(BlePairingCandidate(btDevice))
        }
    }

    suspend fun pairViaBle(candidate: BlePairingCandidate): Boolean {
        LogCat.d("BLE pairViaBle start mac=${candidate.mac}")
        val btDevice = try {
            BluetoothUtil.findOneAsync(candidate.mac) ?: run {
                LogCat.e("BLE pairViaBle findOneAsync returned null")
                return false
            }
        } catch (e: Exception) {
            LogCat.e("PairingTransport findOneAsync error: ${e.message}")
            return false
        }
        LogCat.d("BLE pairViaBle got btDevice, connecting")
        return try {
            BluetoothUtil.connect(btDevice)
            if (!waitForConnection(btDevice)) {
                LogCat.e("BLE pairViaBle waitForConnection timeout")
                return false
            }
            LogCat.d("BLE pairViaBle connected, requestMtu")
            BluetoothUtil.requestMtu(btDevice, MTU_REQUEST)
            val api = BluetoothApi("pairingService", PairingGattServer.SERVICE_UUID, PairingGattServer.PAIRING_CHAR_UUID)
            LogCat.d("BLE pairViaBle readRequest")
            val peerRequest = readRequest(btDevice, api) ?: run {
                LogCat.e("BLE pairViaBle readRequest returned null")
                return false
            }
            LogCat.d("BLE pairViaBle read OK, building local request")
            val myKeyPair = CryptoHelper.generateECDHKeyPair()
            val myRequest = buildLocalRequest(myKeyPair)
            LogCat.d("BLE pairViaBle local request built, writing")
            if (!writeRequest(btDevice, api, myRequest)) {
                LogCat.e("BLE pairViaBle writeRequest returned false")
                return false
            }
            LogCat.d("BLE pairViaBle write OK, teardown")
            BluetoothUtil.teardownConnection(btDevice)
            if (!PairingSecurity.validateTimestamp(peerRequest.timestamp)) {
                LogCat.e("BLE peer request timestamp invalid")
                return false
            }
            if (!PairingSecurity.verify(peerRequest)) {
                LogCat.e("BLE peer request signature invalid")
                return false
            }
            val peerEcdhBytes = Base64.decode(peerRequest.ecdhPublicKey, Base64.NO_WRAP)
            val sharedKey = CryptoHelper.computeECDHSharedKey(myKeyPair.private, peerEcdhBytes) ?: return false
            PeerManager.upsertPaired(
                deviceId = peerRequest.fromId,
                deviceName = peerRequest.fromName,
                deviceIps = peerRequest.ips,
                port = peerRequest.port,
                deviceType = peerRequest.deviceType,
                key = sharedKey,
                signaturePublicKey = peerRequest.signaturePublicKey,
            )
            LogCat.d("BLE pairViaBle upsertPaired OK")
            true
        } catch (e: Exception) {
            LogCat.e("PairingTransport pairViaBle error: ${e.message} ${e.javaClass.simpleName}")
            false
        }
    }

    private suspend fun buildLocalRequest(keyPair: KeyPair): DPairingRequest {
        val ecdhPublicBase64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        val timestamp = System.currentTimeMillis()
        val request = DPairingRequest(
            fromId = TempData.clientId,
            fromName = TempData.deviceName.value,
            port = TempData.httpsPort.value,
            deviceType = PhoneHelper.getDeviceType(appContext),
            ecdhPublicKey = ecdhPublicBase64,
            signaturePublicKey = SignatureHelper.getRawPublicKeyBase64Async(),
            timestamp = timestamp,
            ips = NetworkHelper.getDeviceIP4s().toList(),
        )
        request.signature = SignatureHelper.signTextAsync(request.toSignatureData())
        return request
    }

    private suspend fun handlePeerRequest(peerRequest: DPairingRequest) {
        try {
            if (!PairingSecurity.validateTimestamp(peerRequest.timestamp)) {
                LogCat.e("BLE peer request timestamp invalid (server side)")
                return
            }
            if (!PairingSecurity.verify(peerRequest)) {
                LogCat.e("BLE peer request signature invalid (server side)")
                return
            }
            val kp = localKeyPair ?: return
            val peerEcdhBytes = Base64.decode(peerRequest.ecdhPublicKey, Base64.NO_WRAP)
            val sharedKey = CryptoHelper.computeECDHSharedKey(kp.private, peerEcdhBytes) ?: return
            PeerManager.upsertPaired(
                deviceId = peerRequest.fromId,
                deviceName = peerRequest.fromName,
                deviceIps = peerRequest.ips,
                port = peerRequest.port,
                deviceType = peerRequest.deviceType,
                key = sharedKey,
                signaturePublicKey = peerRequest.signaturePublicKey,
            )
        } catch (e: Exception) {
            LogCat.e("BLE server-side pairing error: ${e.message}")
        }
    }

    private suspend fun waitForConnection(btDevice: BTDevice): Boolean {
        val deadline = System.currentTimeMillis() + CONNECT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (btDevice.isConnected()) return true
            kotlinx.coroutines.delay(200)
        }
        return false
    }

    private suspend fun readRequest(btDevice: BTDevice, api: BluetoothApi): DPairingRequest? {
        BluetoothUtil.enqueueOperation(BTOperationCharacteristicRead(btDevice, api))
        val result = waitForResult(btDevice, BluetoothActionType.CHARACTERISTIC_READ, api.charUUID, READ_TIMEOUT_MS)
            ?: return null
        val json = result.value as? String ?: return null
        return try {
            JsonHelper.jsonDecode<DPairingRequest>(json)
        } catch (e: Exception) {
            LogCat.e("BLE read parse error: ${e.message}")
            null
        }
    }

    private suspend fun writeRequest(btDevice: BTDevice, api: BluetoothApi, request: DPairingRequest): Boolean {
        val json = JsonHelper.jsonEncode(request)
        return try {
            BluetoothUtil.enqueueOperation(BTOperationCharacteristicWrite(btDevice, api, json))
            val ok = waitForWriteAck(btDevice, WRITE_TIMEOUT_MS)
            if (!ok) LogCat.e("BLE write failed")
            ok
        } catch (e: Exception) {
            LogCat.e("BLE write error: ${e.message}")
            false
        }
    }

    private suspend fun waitForWriteAck(btDevice: BTDevice, timeoutMs: Long): Boolean {
        val result = waitForResult(btDevice, BluetoothActionType.CHARACTERISTIC_WRITE, null, timeoutMs)
            ?: return false
        return result.status == BluetoothActionResult.SUCCESS
    }

    private suspend fun waitForResult(
        btDevice: BTDevice,
        type: BluetoothActionType,
        uuid: UUID?,
        timeoutMs: Long,
    ): BluetoothResult? {
        return kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            val channel = btDevice.getChannel(type)
            var result = channel.receive()
            while (uuid != null && result.uuid != uuid) {
                LogCat.e("Got a $type reply for uuid ${result.uuid}, expecting uuid $uuid")
                result = channel.receive()
            }
            result
        }
    }
}

private const val CONNECT_TIMEOUT_MS = 10_000L
private const val READ_TIMEOUT_MS = 10_000L
private const val WRITE_TIMEOUT_MS = 5_000L
private const val MTU_REQUEST = 517

data class BlePairingCandidate(val btDevice: BTDevice) {
    val mac: String = btDevice.mac
    val name: String get() = btDevice.device.name ?: mac
}
