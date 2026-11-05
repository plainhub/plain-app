package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.native.RustBleBridge

object RustBleAdvertisement {
    fun decode(serviceUuids: List<String>): String? {
        val input = JsonHelper.jsonEncode(serviceUuids).encodeToByteArray()
        val result = checkNotNull(RustBleBridge.call(9, 0, 0, 0, 0, false, input))
        return JsonHelper.jsonDecode<String?>(result.decodeToString())
    }
}
