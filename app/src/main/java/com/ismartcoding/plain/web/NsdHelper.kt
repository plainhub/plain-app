package com.ismartcoding.plain.web

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicBoolean
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
    // Prevents concurrent registerServices() calls that would stack up JmDNS threads
    private val registering = AtomicBoolean(false)
    
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
        if (!registering.compareAndSet(false, true)) {
            LogCat.d("registerServices already in progress, skipping")
            return false
        }
        try {
        return registerServicesInternal(context, httpPort, httpsPort)
        } finally {
            registering.set(false)
        }
    }

    private fun registerServicesInternal(context: Context, httpPort: Int?, httpsPort: Int?): Boolean {
        unregisterService(waitForJmDns = true)

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
            } catch (e: OutOfMemoryError) {
                // JmDNS spawns Timer/Thread objects; if the system is out of threads we
                // must not crash — log and give up on this interface.
                LogCat.e("OOM creating JmDNS on $ip (too many threads?): ${e.message}")
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
     * Unregister the service when no longer needed.
     *
     * @param waitForJmDns when true, blocks the calling thread until all JmDNS instances have
     *   been closed.  This MUST be true when called before creating new JmDNS instances so that
     *   old Timer/Thread objects are fully destroyed before new ones are spawned — preventing
     *   thread exhaustion (OOM: pthread_create failed).
     */
    fun unregisterService(waitForJmDns: Boolean = false) {
        val listeners = registrationListeners.toList().also { registrationListeners.clear() }
        val instances = jmDNSInstances.also {
            jmDNSInstances = emptyList()
            jmDNS = null
        }

        // NSD unregister is always fire-and-forget (async via Android's NsdManager).
        listeners.forEach { l ->
            runCatching { nsdManager?.unregisterService(l) }
                .onFailure { LogCat.e("Failed to unregister Android NSD service: ${it.message}") }
        }
        if (listeners.isNotEmpty()) LogCat.d("Unregistered Android NSD service(s): ${listeners.size}")

        if (instances.isEmpty()) return

        if (waitForJmDns) {
            // Block until all JmDNS threads/timers have stopped before the caller creates new
            // ones.  Without this the old Timer threads keep running and we exhaust the OS
            // thread limit, triggering OOM: pthread_create failed.
            runBlocking {
                instances.forEach { j ->
                    runCatching {
                        withTimeout(5_000) {
                            runCatching { j.unregisterAllServices() }
                            runCatching { j.close() }
                        }
                    }
                        .onSuccess { LogCat.d("Closed JmDNS instance (sync)") }
                        .onFailure { LogCat.e("Failed to shutdown JmDNS (sync): ${it.message}") }
                }
            }
        } else {
            // Do NOT cancel a running unregister job. Each call owns its own snapshot of
            // instances so concurrent jobs work on disjoint sets.
            unregisterJob = coIO {
                instances.forEach { j ->
                    runCatching {
                        withTimeout(5_000) {
                            runCatching { j.unregisterAllServices() }
                            runCatching { j.close() }
                        }
                    }
                        .onSuccess { LogCat.d("Closed JmDNS instance (async)") }
                        .onFailure { LogCat.e("Failed to shutdown JmDNS (async): ${it.message}") }
                }
            }
        }
    }
}