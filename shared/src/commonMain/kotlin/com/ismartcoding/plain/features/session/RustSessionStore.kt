package com.ismartcoding.plain.features.session

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.ui.models.VSession
import com.ismartcoding.plain.enums.SessionType
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.jsonObject
import kotlin.time.Instant

object RustSessionStore {
    private suspend fun call(command: SessionCommand) = RustContentApi.postJsonOrThrow("system/sessions", JsonHelper.jsonEncodeToElement<SessionCommand>(command).jsonObject).getValue("result")
    suspend fun key(clientId: String): ByteArray? = JsonHelper.jsonDecodeFromElement<String?>(call(SessionCommand.Key(clientId)))?.let(Base64Lenient::decode)
    suspend fun list(): List<VSession> = JsonHelper.jsonDecodeFromElement<List<SessionFacts>>(call(SessionCommand.List)).map {
        VSession(it.clientId, it.name, SessionType.valueOf(it.type), it.token, it.clientIp, it.osName, it.osVersion,
            it.browserName, it.browserVersion, Instant.fromEpochMilliseconds(it.createdAt), Instant.fromEpochMilliseconds(it.updatedAt),
            it.lastActiveAt?.let(Instant::fromEpochMilliseconds))
    }
    suspend fun create(name: String) { call(SessionCommand.Create(name)) }
    suspend fun rename(clientId: String, name: String): Boolean = JsonHelper.jsonDecodeFromElement<Boolean>(call(SessionCommand.Rename(clientId, name)))
    suspend fun delete(clientId: String) { call(SessionCommand.Delete(clientId)) }
}
