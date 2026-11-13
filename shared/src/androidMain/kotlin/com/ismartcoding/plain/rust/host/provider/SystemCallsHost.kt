package com.ismartcoding.plain.rust.host.provider

import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Call as PhoneCall
import com.ismartcoding.plain.platform.media.call
import com.ismartcoding.plain.platform.media.countMedia
import com.ismartcoding.plain.platform.media.getMediaIds
import com.ismartcoding.plain.platform.media.searchMedia
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.host.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

internal object SystemCallsHost {
    internal suspend fun handle(method: String, params: JsonObject): HostResult = when (method) {
        "systemCallFacts" -> {
            val request = JsonHelper.jsonDecodeFromElement<ProviderPageRequest>(params)
            hostResult(
                searchMedia(
                    DataType.CALL, request.plan, request.limit, request.offset,
                    FileSortBy.DATE_DESC,
                ).filterIsInstance<PhoneCall>().map(::callFacts)
            )
        }
        "systemCallCount" -> hostResult(
            countMedia(
                DataType.CALL,
                params.providerPlan(),
            )
        )
        "systemCallIds" -> hostResult(
            getMediaIds(
                DataType.CALL,
                params.providerPlan(),
            )
        )
        "systemMakeCall" -> {
            val request = JsonHelper.jsonDecodeFromElement<MakeCallRequest>(params)
            call(
                request.number, request.showDialer,
            )
            hostResult(true)
        }
        else -> error("Unsupported provider operation")
    }

    private fun callFacts(call: PhoneCall) = CallFacts(
        id = call.id, number = call.number, name = call.name, photoUri = call.photo,
        startedAt = call.startedAt.toString(), durationSec = call.durationSec, type = call.typeCode, accountId = call.accountId,
    )
}

@Serializable
private data class MakeCallRequest(val number: String, val showDialer: Boolean)
