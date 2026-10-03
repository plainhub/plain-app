@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.ismartcoding.plain.platform

import io.ktor.http.decodeURLPart
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.lib.withIO
import platform.Foundation.NSFileManager

internal actual suspend fun serveRustWebAsset(call: HttpCall): Boolean = withIO {
    val requested = call.path.removePrefix("/").decodeURLPart()
    if (requested.split('/').any { it == ".." || it == "." } || requested.contains('\\')) return@withIO false
    val resource = if (requested.isEmpty() || !requested.contains('.')) "index.html" else requested
    val path = IosWebAssets.resolve(resource) ?: return@withIO false
    call.responseHeader("Cache-Control", if (requested.startsWith("assets/")) "public, max-age=31536000" else "no-cache, no-store")
    if (resource == "index.html") {
        val bytes = NSFileManager.defaultManager.contentsAtPath(path)?.toByteArray() ?: return@withIO false
        val html = bytes.decodeToString().replace("<head>", "<head><script>window.__SERVER_TIME__=${TimeHelper.nowMillis()}</script>")
        call.respondText(html, "text/html; charset=utf-8")
    } else call.respondFile(path, IosWebAssets.contentTypeFor(resource))
    true
}
