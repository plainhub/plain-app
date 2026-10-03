package com.ismartcoding.plain.platform

import com.ismartcoding.plain.httpserver.http.HttpCall

internal expect suspend fun serveRustWebAsset(call: HttpCall): Boolean
