package com.ismartcoding.plain.web

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeout
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

object NsdHelper {
    private const val SERVICE_TYPE_HTTP = "_http._tcp.local."
    private const val SERVICE_TYPE_HTTPS = "_https._tcp.local."
    private const val SERVICE_NAME = "PlainApp"
    
    private var nsdManager: NsdManager? = null
    // Only listeners whose onServiceRegistered callback has fired are stored here.
    // This prevents attempting to unregister listeners whose NSD registration failed,
    // which would throw "listener not registered".
    private val registrationListeners = mutableListOf<NsdManager.RegistrationListener>()
    private var jmDNS: JmDNS? = null
    // All JmDNS instances (one per physical network interface)
    private var jmDNSInstances: List<JmDNS> = emptyList()
    private var unregisterJob: Job? = null
    
    private data class ServiceDescriptor(
        val type: String,
        val name: String,
        val port: Int,
        val description: String,
        val attributes: Map<String, String> = emptyMap(),
    )

    /**
     * Backwards-compatible wrapper: registers only the HTTP service.
     */
    fun registerService(context: Context, port: Int): Boolean {
        return registerServices(context, httpPort = port, httpsPort = null)
    }

    /**
     * Register both HTTP and HTTPS services with Android NSD and JmDNS.
     * Returns true if at least one registration path succeeded.
     */
    fun registerServices(context: Context, httpPort: Int?, httpsPort: Int?): Boolean {
        unregisterService()

        val hostname = TempData.mdnsHostname
        val services = buildList {
            if (httpPort != null && httpPort > 0) {
                add(
                    ServiceDescriptor(
                        type = SERVICE_TYPE_HTTP,
                        name = SERVICE_NAME,
                        port = httpPort,
                        description = "Plain App HTTP Web Service",
                        attributes = mapOf(
                            "path" to "/",
                            "hostname" to hostname,
                            "scheme" to "http",
                        ),
                    )
                )
            }

            if (httpsPort != null && httpsPort > 0) {
                add(
                    ServiceDescriptor(
                        type = SERVICE_TYPE_HTTPS,
                        name = SERVICE_NAME,
                        port = httpsPort,
                        description = "Plain App HTTPS Web Service",
                        attributes = mapOf(
                            "path" to "/",
                            "hostname" to hostname,
                            "scheme" to "https",
                        ),
                    )
                )
            }
        }

        var androidOk = false
        var jmdnsOk = false

        if (services.isEmpty()) {
            LogCat.e("No services to register (ports missing)")
            return false
        }

        // Register with Android NSD
        androidOk = registerWithAndroidNsd(context, services)

        // Register with JmDNS for better mDNS support
        jmdnsOk = registerWithJmDNS(services)

        return androidOk || jmdnsOk
    }

    private fun registerWithAndroidNsd(context: Context, services: List<ServiceDescriptor>): Boolean {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

        var ok = false
        for (service in services) {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = service.name
                serviceType = service.type
                port = service.port
                service.attributes.forEach { (k, v) ->
                    if (v.isNotEmpty()) setAttribute(k, v)
                }
            }

            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    // Add to the tracked list only after confirmed registration so that
                    // unregisterService() never attempts to unregister a failed listener.
                    registrationListeners.add(this)
                    LogCat.d("NSD service registered: ${serviceInfo.serviceType} ${serviceInfo.serviceName}")
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    // Do NOT add to registrationListeners; nothing to unregister.
                    LogCat.e("NSD registration failed: ${serviceInfo.serviceType} error code $errorCode")
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    LogCat.d("NSD service unregistered: ${serviceInfo.serviceType} ${serviceInfo.serviceName}")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    LogCat.e("NSD unregistration failed: ${serviceInfo.serviceType} error code $errorCode")
                }
            }

            try {
                nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
                ok = true
                LogCat.d("Registering Android NSD service ${service.type} on port ${service.port}")
            } catch (e: Exception) {
                LogCat.e("Failed to register Android NSD service ${service.type}: ${e.message}")
            }
        }

        return ok
    }

    private fun registerWithJmDNS(services: List<ServiceDescriptor>): Boolean {
        // Collect all physical (non-VPN) IPv4 addresses.  mDNS is link-local and must be
        // announced on each interface separately; a single JmDNS bound to one IP won't be
        // reachable from other subnets (Wi-Fi VLAN, Ethernet, etc.).
        val ips = NetworkHelper.getDeviceIP4s().filter { ip ->
            try {
                val ni = NetworkInterface.getByInetAddress(InetAddress.getByName(ip))
                ni != null && !NetworkHelper.isVpnInterface(ni.name)
            } catch (_: Exception) {
                false
            }
        }

        if (ips.isEmpty()) {
            LogCat.e("Failed to get any physical device IP for JmDNS")
            return false
        }

        var anyOk = false
        val instances = mutableListOf<JmDNS>()
        for (ip in ips) {
            try {
                val addr = InetAddress.getByName(ip)
                val instance = JmDNS.create(addr, TempData.mdnsHostname)
                for (service in services) {
                    val info = ServiceInfo.create(
                        service.type,
                        service.name,
                        service.port,
                        service.description
                    )
                    instance.registerService(info)
                }
                instances.add(instance)
                LogCat.d("Registered JmDNS service on $ip (${TempData.mdnsHostname})")
                anyOk = true
            } catch (e: Exception) {
                LogCat.e("Failed to register JmDNS on $ip: ${e.message}")
            }
        }
        // Store only the first instance for legacy unregister path; extra instances are
        // kept in jmDNSInstances and cleaned up in unregisterService().
        jmDNS = instances.firstOrNull()
        jmDNSInstances = instances
        return anyOk
    }
    
    /**
     * Unregister the service when no longer needed
     */
    fun unregisterService() {
        val listeners = registrationListeners.toList().also { registrationListeners.clear() }
        val instances = jmDNSInstances.also {
            jmDNSInstances = emptyList()
            jmDNS = null
        }

        // Do NOT cancel a running unregister job. Each call owns its own snapshot of
        // listeners (taken above) so concurrent jobs work on disjoint sets — cancelling
        // the previous job would leave its listeners permanently registered with NSD.

        unregisterJob = coIO {
            listeners.forEach { l ->
                runCatching { nsdManager?.unregisterService(l) }
                    .onFailure { LogCat.e("Failed to unregister Android NSD service: ${it.message}") }
            }
            if (listeners.isNotEmpty()) LogCat.d("Unregistered Android NSD service(s): ${listeners.size}")

            instances.forEach { j ->
                runCatching {
                    withTimeout(5_000) {
                        runCatching { j.unregisterAllServices() }
                        runCatching { j.close() }
                    }
                }
                    .onSuccess { LogCat.d("Unregistered JmDNS instance") }
                    .onFailure { LogCat.e("Failed to shutdown JmDNS: ${it.message}") }
            }
        }
    }
}