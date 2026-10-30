package com.ismartcoding.plain.tests

import android.net.NetworkCapabilities
import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.NetworkMonitor
import com.ismartcoding.plain.connectivityManager
import com.ismartcoding.plain.features.nearby.RustMdnsRuntime
import com.ismartcoding.plain.platform.network.NetworkEvents
import com.ismartcoding.plain.platform.network.NetworkType
import com.ismartcoding.plain.rust.native.RustNativeApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NetworkInterfaceLogTest {
    @Test
    fun androidNetworkFactsRetainWifiPriorityAndMdnsDiscovery() = runBlocking {
        withTimeout(5_000) { NetworkEvents.snapshot.first { it.lanIpv4Addresses != null } }
        val network = connectivityManager.activeNetwork
        val properties = network?.let(connectivityManager::getLinkProperties)
        val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
        if (properties?.interfaceName != null && capabilities != null) {
            val facts = withTimeout(5_000) {
                var fact = NetworkMonitor.interfaceFacts().find { it.name == properties.interfaceName }
                while (fact == null) {
                    delay(50)
                    fact = NetworkMonitor.interfaceFacts().find { it.name == properties.interfaceName }
                }
                fact
            }
            assertEquals(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN), facts.vpn)
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                assertEquals(NetworkType.WIFI, facts.networkType)
                if (!facts.vpn) {
                    val wifiAddresses = properties.linkAddresses.mapNotNull { (it.address as? Inet4Address)?.hostAddress }
                    if (wifiAddresses.isNotEmpty()) {
                        assertTrue("Wi-Fi addresses must precede other transports", RustNativeApi.lanIpv4Addresses().first() in wifiAddresses)
                    }
                }
            }
        }
        val token = UUID.randomUUID().toString()
        val before = RustMdnsRuntime.snapshot()
        try {
            val active = RustMdnsRuntime.debugStart(token)
            assertTrue(active.receiver)
            assertTrue(active.scanning)
            withTimeout(5_000) {
                while (RustMdnsRuntime.snapshot().packetsOut.none { it.summary.contains("PTR") }) delay(50)
            }
        } finally {
            val after = RustMdnsRuntime.debugStop(token).await()
            assertEquals(before.manualScanning, after.manualScanning)
            assertEquals(before.capturing, after.capturing)
        }
    }

    @Test
    fun nativeAddressEnumerationPreservesAddressesWithoutRestrictedSysfsAccess() = runBlocking {
        val marker = "network-interface-log-${UUID.randomUUID()}"
        Log.i("PlainNetworkTest", marker)
        val expected = NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isPointToPoint }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .filterNot { it.isAnyLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress || it.isMulticastAddress }
            .map { it.hostAddress }
            .toSet()
        repeat(4) {
            val actual = RustNativeApi.lanIpv4Addresses()
            assertTrue("Native addresses must retain active IPv4 interfaces", actual.containsAll(expected))
            delay(250)
        }
        val reader = ProcessBuilder("logcat", "-d", "--pid=${Process.myPid()}", "-v", "brief").start()
        val output = reader.inputStream.bufferedReader().use { it.readText() }
        assertTrue("Log reader must succeed", reader.waitFor() == 0)
        assertTrue("Log marker must be present", output.contains(marker))
        val denied = output.substringAfter(marker).lineSequence()
            .count { it.contains("avc:") && it.contains("sysfs_net") }
        assertTrue("Network enumeration caused $denied restricted sysfs accesses", denied == 0)
    }
}
