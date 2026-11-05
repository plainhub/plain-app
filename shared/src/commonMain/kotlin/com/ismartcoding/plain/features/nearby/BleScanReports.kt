package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.rust.native.RustHostReports

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.Serializable


object BleScanReports {
    suspend fun scanState(id: String, active: Boolean) {
        RustHostReports.bleScanState(JsonHelper.jsonEncode(ScanState(id, active)))
    }
    suspend fun scanSeen(id: String, clientId: String) {
        RustHostReports.bleAdvertisement(JsonHelper.jsonEncode(ScanSeen(id, clientId)))
    }
}

@Serializable
private data class ScanState(val scanId: String, val active: Boolean)

@Serializable
private data class ScanSeen(val scanId: String, val clientId: String)
