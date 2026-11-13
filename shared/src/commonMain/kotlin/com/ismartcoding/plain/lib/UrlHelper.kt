package com.ismartcoding.plain.lib

object UrlHelper {
    /**
     * Build `scheme://host[:port][/path]`. The port is omitted when it is the
     * scheme default (http: 80, https/wss: 443).
     */
    fun buildUrl(scheme: String, host: String, port: Int, path: String = ""): String {
        val portPart = if ((scheme == "http" && port == 80) || (scheme == "https" || scheme == "wss") && port == 443) "" else ":$port"
        val authority = if (host.contains(':')) "[$host]" else host
        return "$scheme://$authority$portPart$path"
    }

    fun getPolicyUrl(): String {
        return "https://plainapp.app/privacy"
    }

    fun getTermsUrl(): String {
        return "https://plainapp.app/terms"
    }
}
