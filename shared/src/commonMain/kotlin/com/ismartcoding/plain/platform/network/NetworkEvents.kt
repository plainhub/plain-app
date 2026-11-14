package com.ismartcoding.plain.platform.network

import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.RustRuntime
import com.ismartcoding.plain.rust.native.RustHostReports
import com.ismartcoding.plain.rust.native.RustNativeApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class NetworkSnapshot(val vpnActive: Boolean, val lanIpv4Addresses: List<String>?)

object NetworkEvents {
    private val mutex = Mutex()
    private val mutableSnapshot = MutableStateFlow(NetworkSnapshot(vpnActive(), null))
    val snapshot = mutableSnapshot.asStateFlow()

    fun changed() {
        coIO {
            mutex.withLock {
                if (!RustRuntime.isCoreInitialized) {
                    mutableSnapshot.value = mutableSnapshot.value.copy(vpnActive = vpnActive())
                    return@withLock
                }
                try {
                    RustHostReports.networkInterfacesChanged("null")
                    refreshSnapshot()
                } catch (canceled: CancellationException) { throw canceled }
                catch (error: Exception) { LogCat.e("Network change report failed", error) }
            }
        }
    }

    suspend fun refresh() = mutex.withLock { refreshSnapshot() }

    private suspend fun refreshSnapshot() {
        mutableSnapshot.value = NetworkSnapshot(vpnActive(), RustNativeApi.lanIpv4Addresses())
    }
}
