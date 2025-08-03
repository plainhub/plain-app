package com.ismartcoding.plain.web

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

object NsdHelper {
    private const val SERVICE_TYPE = "_http._tcp.local."
    private const val SERVICE_NAME = "PlainApp"
    
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var jmDNS: JmDNS? = null
    
    /**
     * Register the HTTP service with NSD and mDNS
     */
    fun registerService(context: Context, port: Int) {
        unregisterService()
        
        // Register with Android NSD
        registerWithAndroidNsd(context, port)
        
        // Register with JmDNS for better mDNS support
        registerWithJmDNS(port)
    }
    
    private fun registerWithAndroidNsd(context: Context, port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("path", "/")
            setAttribute("hostname", TempData.mdnsHostname)
        }
        
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                // The service has been registered
                LogCat.d("NSD service registered: ${serviceInfo.serviceName}")
            }
            
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                LogCat.e("NSD registration failed: error code $errorCode")
            }
            
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                LogCat.d("NSD service unregistered: ${serviceInfo.serviceName}")
            }
            
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                LogCat.e("NSD unregistration failed: error code $errorCode")
            }
        }
        
        try {
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            LogCat.d("Registering Android NSD service on port $port")
        } catch (e: Exception) {
            LogCat.e("Failed to register Android NSD service: ${e.message}")
        }
    }
    
    private fun registerWithJmDNS(port: Int) {
        try {
            val ip = NetworkHelper.getDeviceIP4()
            if (ip.isEmpty()) {
                LogCat.e("Failed to get device IP for JmDNS")
                return
            }
            
            val addr = InetAddress.getByName(ip)
            jmDNS = JmDNS.create(addr, TempData.mdnsHostname)
            
            val serviceInfo = ServiceInfo.create(
                SERVICE_TYPE,
                SERVICE_NAME,
                port,
                "Plain App Web Service"
            )
            
            jmDNS?.registerService(serviceInfo)
            LogCat.d("Registered JmDNS service on ${TempData.mdnsHostname}:$port")
        } catch (e: Exception) {
            LogCat.e("Failed to register JmDNS service: ${e.message}")
        }
    }
    
    /**
     * Unregister the service when no longer needed
     */
    fun unregisterService() {
        // Unregister Android NSD
        registrationListener?.let { listener ->
            try {
                nsdManager?.unregisterService(listener)
                LogCat.d("Unregistered Android NSD service")
            } catch (e: Exception) {
                LogCat.e("Failed to unregister Android NSD service: ${e.message}")
            }
        }
        registrationListener = null
        
        // Unregister JmDNS
        try {
            jmDNS?.unregisterAllServices()
            jmDNS?.close()
            LogCat.d("Unregistered JmDNS service")
        } catch (e: Exception) {
            LogCat.e("Failed to unregister JmDNS service: ${e.message}")
        }
        jmDNS = null
    }
}