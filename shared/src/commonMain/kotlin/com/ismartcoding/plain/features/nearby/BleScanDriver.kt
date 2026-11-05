package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.Serializable

import com.ismartcoding.plain.platform.network.ble.RustBleWire
import com.ismartcoding.plain.platform.network.ble.BleServices
import com.ismartcoding.plain.platform.network.ble.client.BleDeviceApi
import com.ismartcoding.plain.platform.network.ble.client.BleGattClient
import com.ismartcoding.plain.platform.network.ble.client.BleScanner
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.network.bleTransport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.io.encoding.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object BleScanDriver {
    suspend fun scan() {
        try { feedScan() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Throwable) { LogCat.e("BLE scan driver", error) }
    }

    private suspend fun feedScan() {
        val scan = Uuid.random().toString()
        try {
            BleScanReports.scanState(scan, true)
            forEachAdvertisement { clientId ->
                try { BleScanReports.scanSeen(scan, clientId) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.d("[BLE] scanSeen $clientId: ${error.message}") }
            }
        } finally {
            withContext(NonCancellable) { BleScanReports.scanState(scan, false) }
        }
    }

    private suspend fun forEachAdvertisement(onSeen: suspend (String) -> Unit) {
        bleTransport().createScanner().scan().collect { device ->
            onSeen(device.id)
        }
    }

    suspend fun read(params: JsonObject): JsonElement {
        val input = JsonHelper.jsonDecodeFromElement<ScanReadRequest>(params)
        val clientId = input.clientId
        val request = Base64.decode(input.request)
        val scanner = bleTransport().createScanner()
        val device = scanner.createClient(clientId)
            ?: return JsonHelper.jsonEncodeToElement(ScanReadReply(null))
        val payload = readReply(scanner, device, request)
        return JsonHelper.jsonEncodeToElement(ScanReadReply(payload))
    }

    private suspend fun readReply(scanner: BleScanner, device: BleGattClient, request: ByteArray): String? {
        return try {
            scanner.pauseScan()
            val api = BleDeviceApi(device, RustBleWire)
            api.ensureConnected()
            if (!api.isConnected()) return null
            val result = api.requestAsync(BleServices.nearby, request)
            api.disconnect()
            RustBleWire.nearbyBody(result).decodeToString(throwOnInvalidSequence = true).takeIf { it.isNotEmpty() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            LogCat.e("[BLE] bleScanRead error: ${e.message}")
            null
        } finally {
            scanner.resumeScan()
            scanner.teardownConnection(device)
        }
    }
}

@Serializable
private data class ScanReadRequest(val clientId: String, val request: String)

@Serializable
private data class ScanReadReply(val payload: String?)
