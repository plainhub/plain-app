package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.PasswordType
import com.ismartcoding.plain.enums.SessionType
import com.ismartcoding.plain.events.WebRequestReceivedEvent
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.preferences.SystemPrefs
import kotlinx.serialization.json.*

object MainGraphQLHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "mainGraphqlHealth" -> JsonPrimitive(com.ismartcoding.plain.platform.getOwnPackageName())
        "mainGraphqlShutdown" -> {
            com.ismartcoding.plain.httpserver.closeAllWsSessions()
            com.ismartcoding.plain.lib.coIO {
                kotlinx.coroutines.delay(100)
                com.ismartcoding.plain.platform.finishHttpServerStopAsync()
            }
            JsonNull
        }
        "mainGraphqlInitFacts" -> buildJsonObject {
            put("desktopAccessEnabled", TempData.canDesktopAccess())
            val key = HttpServerManager.tokenCache.get(params.getValue("clientId").jsonPrimitive.content)
            put("tokenKey", key?.let { JsonPrimitive(Base64Lenient.encode(it)) } ?: JsonNull)
        }
        "mainGraphqlInitResponse" -> {
            val clientId = params.getValue("clientId").jsonPrimitive.content
            HttpServerManager.clientIpCache.put(clientId, params.getValue("remoteHost").jsonPrimitive.content)
            val resetPassword = params.getValue("resetPassword").jsonPrimitive.boolean
            val password = if (resetPassword && SystemPrefs.passwordTypeValue() == PasswordType.NONE) {
                HttpServerManager.resetPasswordAsync()
            } else {
                ""
            }
            buildJsonObject {
                put("signaturePublicKey", SignatureHelper.getRawPublicKeyBase64Async())
                put("password", password)
            }
        }
        "mainGraphqlAuthFacts" -> {
            val clientId = params.getValue("clientId").jsonPrimitive.content
            val session = SessionList.getByClientIdAsync(clientId)
            val key = HttpServerManager.tokenCache.get(clientId)
            buildJsonObject {
                put("desktopAccessEnabled", TempData.canDesktopAccess())
                put("tokenKey", key?.let { JsonPrimitive(Base64Lenient.encode(it)) } ?: JsonNull)
                put("customSessionToken", if (session?.type == SessionType.CUSTOM) session.token else null)
            }
        }
        else -> error("Unsupported main GraphQL host operation")
    }
}
