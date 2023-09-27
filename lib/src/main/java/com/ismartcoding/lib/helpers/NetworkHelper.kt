package com.ismartcoding.lib.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.common.net.InternetDomainName
import java.net.*
import java.util.*
import java.util.regex.Pattern
import kotlin.experimental.and
import kotlin.random.Random

object NetworkHelper {
    private const val IP4_PATTERN =
        "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"

    private const val IP6_PATTERN =
        "^s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:)))(%.+)?s*$"

    private const val PORT_OR_RANGE_PATTERN = "[0-9]{1,5}|([0-9]{1,5}\\-[0-9]{1,5})"

    private val ip4Pattern = Pattern.compile(IP4_PATTERN)
    private val ip6Pattern = Pattern.compile(IP6_PATTERN)
    private val portOrRangePattern = Pattern.compile(PORT_OR_RANGE_PATTERN)

    fun getNetworkInterfaces(): List<NetworkInterface> {
        return try {
            Collections.list(NetworkInterface.getNetworkInterfaces())
        } catch (ex: java.lang.Exception) {
            arrayListOf()
        }
    }

    fun getDeviceIP4(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            val map = mutableMapOf<String, String>()
            while (en?.hasMoreElements() == true) {
                val intf = en.nextElement()
                if (intf.isUp) {
                    val enumIpAddr = intf.inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            map[intf.name] = inetAddress.getHostAddress() ?: ""
                        }
                    }
                }
            }
            if (map.isNotEmpty()) {
                return map["wlan0"] ?: map.values.first()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return ""
    }

    fun getDeviceIP4s(): Set<String> {
        val ips = mutableSetOf<String>()
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            val map = mutableMapOf<String, String>()
            while (en?.hasMoreElements() == true) {
                val intf = en.nextElement()
                if (intf.isUp) {
                    val enumIpAddr = intf.inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            val ip = inetAddress.getHostAddress() ?: ""
                            if (ip.isNotEmpty()) {
                                ips.add(ip)
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return ips
    }

    fun isVPNConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networks = cm.allNetworks
        for (i in networks.indices) {
            val caps = cm.getNetworkCapabilities(networks[i])
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                return true
            }
        }
        return false
    }

    fun subnetContains(
        subnet: String,
        ip: String,
    ): Boolean {
        val parts1 = subnet.split("/")
        val mask = maskLengthToIP4(parts1[1].toInt())
        return try {
            isSubnetOverlapped(mask, parts1[0], mask, ip)
        } catch (e: Exception) {
            false
        }
    }

    fun randomFirstIP(type: String = "A"): String {
        if (type == "A") {
            return "10.${Random.nextInt(0, 255)}.${Random.nextInt(0, 255)}.1"
        } else if (type == "B") {
            return "172.${Random.nextInt(16, 31)}.${Random.nextInt(0, 255)}.1"
        }

        return "192.168.${Random.nextInt(0, 255)}.1"
    }

    fun isSubnetOverlapped(
        mask1: String,
        ip1: String,
        mask2: String,
        ip2: String,
    ): Boolean {
        return try {
            val maskInt1 = ip4ToLong(mask1)
            val maskInt2 = ip4ToLong(mask2)

            val maskToUse = if (maskInt1 < maskInt2) maskInt1 else maskInt2
            val addr1 = ip4ToLong(ip1) and maskToUse
            val addr2 = ip4ToLong(ip2) and maskToUse

            return addr1 == addr2
        } catch (e: Exception) {
            false
        }
    }

    fun maskLengthToIP4(maskLength: Int): String {
        return longToIP4(maskLengthToLong(maskLength))
    }

    fun maskLengthToLong(maskLength: Int): Long {
        val full: Long = 0xFFFFFFFF
        val subnet: Long = (0x01.toLong() shl (32 - maskLength)) - 1
        return full xor subnet
    }

    fun isIP4(input: String): Boolean {
        return checkPattern(input, ip4Pattern)
    }

    fun isIP6(input: String): Boolean {
        return checkPattern(input, ip6Pattern)
    }

    fun isIP4Net(input: String): Boolean {
        val split = input.split('/')
        if (split.size != 2) {
            return false
        }

        val len = split[1].toIntOrNull()
        return isIP4(split[0]) && len != null && len >= 0 && len <= 32
    }

    fun isIP6Net(input: String): Boolean {
        val split = input.split('/')
        if (split.size != 2) {
            return false
        }

        val len = split[1].toIntOrNull()
        return isIP6(split[0]) && len != null && len > 32 && len <= 128
    }

    fun isIP(input: String): Boolean {
        if (!isIP4(input)) {
            return isIP6(input)
        }

        return true
    }

    fun isIPNet(input: String): Boolean {
        if (!isIP4Net(input)) {
            return isIP6Net(input)
        }

        return true
    }

    fun isPrivateIP6(ip: String): Boolean {
        try {
            val a = Inet6Address.getByName(ip)
            val isUniqueLocal = a.address[0].and(1.toByte()) == 1.toByte() // https://tools.ietf.org/html/rfc4193
            return a.isLinkLocalAddress || isUniqueLocal || a.isSiteLocalAddress
        } catch (e: Exception) {
        }
        return false
    }

    fun isSiteLocalIP4(ip: String): Boolean {
        try {
            val a = Inet4Address.getByName(ip)
            return a.isSiteLocalAddress
        } catch (e: Exception) {
        }
        return false
    }

    fun isPrivateIP4(ip: String): Boolean {
        try {
            val a = Inet4Address.getByName(ip)
            return a.isLoopbackAddress || a.isSiteLocalAddress
        } catch (e: Exception) {
        }
        return false
    }

    fun isIPWithOptionalPort(input: String): Boolean {
        if (input.contains(":")) {
            if (isIP6(input)) {
                return true
            }

            val ip: String
            val port: String
            if (input.contains("]:") && input.startsWith("[")) {
                val split = input.split("]:")
                ip = split[0].trimStart('[')
                port = split[1]
            } else {
                val split = input.split(":")
                ip = split[0]
                port = split[1]
            }

            return isIP(ip) && isPortOrPortRangeMultiple(port)
        }

        return isIP(input)
    }

    fun isPortOrPortRange(input: String): Boolean {
        if (checkPattern(input, portOrRangePattern)) {
            return try {
                if (input.contains("-")) {
                    val parts = input.split("-")
                    val left = parts[0].toInt()
                    val right = parts[1].toInt()
                    left in 1 until right && left < 65536 && right > 0 && right < 65536
                } else {
                    input.toInt() in 1..65535
                }
            } catch (e: Exception) {
                false
            }
        }
        return false
    }

    fun isPort(input: String): Boolean {
        return try {
            input.toInt() in 1..65535
        } catch (e: Exception) {
            false
        }
    }

    fun isNetWithOptionalPort(input: String): Boolean {
        if (input.contains(":")) {
            if (isIP6Net(input)) {
                return true
            }

            val ip: String
            val port: String
            if (input.contains("]:") && input.startsWith("[")) {
                val split = input.split("]:")
                ip = split[0].trimStart('[')
                port = split[1]
            } else {
                val split = input.split(":")
                ip = split[0]
                port = split[1]
            }

            return isIPNet(ip) && isPortOrPortRangeMultiple(port)
        }

        return isIPNet(input)
    }

    fun isDomainWithOptionalPort(input: String): Boolean {
        if (input.contains(":")) {
            val split = input.split(":")
            val domain = split[0]
            val port = split[1]

            return isDomainName(domain) && isPortOrPortRangeMultiple(port)
        }

        return isDomainName(input)
    }

    fun isDomainName(input: String): Boolean {
        return try {
            InternetDomainName.from(input.trimStart('*', '.'))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isPortOrPortRangeMultiple(input: String): Boolean {
        if (input.contains(",")) {
            val split = input.split(",")
            return split.all { isPortOrPortRange(it) }
        }

        return isPortOrPortRange(input)
    }

    private fun longToIP4(ip: Long): String {
        return "${((ip shr 24) % 256)}.${((ip shr 16) % 256)}.${((ip shr 8) % 256)}.${(ip % 256)}"
    }

    private fun ip4ToLong(ip: String): Long {
        val octets = ip.split(".")
        return (
            (octets[0].toLong() shl 24) + (octets[1].toInt() shl 16) +
                (octets[2].toInt() shl 8) + octets[3].toInt()
        )
    }

    private fun checkPattern(
        input: String,
        pattern: Pattern,
    ): Boolean {
        return if (input.isEmpty()) {
            false
        } else {
            try {
                pattern.matcher(input).matches()
            } catch (e: Exception) {
                false
            }
        }
    }
}
