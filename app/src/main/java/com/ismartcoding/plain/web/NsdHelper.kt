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
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

object NsdHelper {
    private const val SERVICE_TYPE_HTTP = "_http._tcp.local."
    private const val SERVICE_TYPE_HTTPS = "_https._tcp.local."
    private const val SERVICE_NAME = "PlainApp"
    
    private var nsdManager: NsdManager? = null
    private val registrationListeners = mutableListOf<NsdManager.RegistrationListener>()
    private var jmDNS: JmDNS? = null
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
                    LogCat.d("NSD service registered: ${serviceInfo.serviceType} ${serviceInfo.serviceName}")
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
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
                registrationListeners.add(listener)
                ok = true
                LogCat.d("Registering Android NSD service ${service.type} on port ${service.port}")
            } catch (e: Exception) {
                LogCat.e("Failed to register Android NSD service ${service.type}: ${e.message}")
            }
        }

        return ok
    }

    private fun registerWithJmDNS(services: List<ServiceDescriptor>): Boolean {
        try {
            val ip = NetworkHelper.getDeviceIP4()
            if (ip.isEmpty()) {
                LogCat.e("Failed to get device IP for JmDNS")
                return false
            }

            val addr = InetAddress.getByName(ip)
            jmDNS = JmDNS.create(addr, TempData.mdnsHostname)

            for (service in services) {
                val info = ServiceInfo.create(
                    service.type,
                    service.name,
                    service.port,
                    service.description
                )
                jmDNS?.registerService(info)
                LogCat.d("Registered JmDNS service ${service.type} on ${TempData.mdnsHostname}:${service.port}")
            }
            return true
        } catch (e: Exception) {
            LogCat.e("Failed to register JmDNS service: ${e.message}")
            return false
        }
    }
    
    /**
     * Unregister the service when no longer needed
     */
    fun unregisterService() {
        val listeners = registrationListeners.toList().also { registrationListeners.clear() }
        val jmdns = jmDNS.also { jmDNS = null }

        unregisterJob?.cancel()

        unregisterJob = coIO {
            listeners.forEach { l ->
                runCatching { nsdManager?.unregisterService(l) }
                    .onFailure { LogCat.e("Failed to unregister Android NSD service: ${it.message}") }
            }
            if (listeners.isNotEmpty()) LogCat.d("Unregistered Android NSD service(s): ${listeners.size}")

            jmdns?.let { j ->
                runCatching {
                    withTimeout(5_000) {
                        runCatching { j.unregisterAllServices() }
                        runCatching { j.close() }
                    }
                }
                    .onSuccess { LogCat.d("Unregistered JmDNS service") }
                    .onFailure { LogCat.e("Failed to shutdown JmDNS: ${it.message}") }
            }
        }
    }
}