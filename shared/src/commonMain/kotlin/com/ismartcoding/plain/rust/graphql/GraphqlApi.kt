package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.BusinessException
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.PeerSubscriptionTarget
import com.ismartcoding.plain.rust.native.RustNativeBindings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object GraphqlApi : GraphqlClient("native", ::executeNativeGraphql) {
    fun forPeer(peer: com.ismartcoding.plain.models.Peer): GraphqlClient = PeerHttpClient(peer).graphql
}

private suspend fun executeNativeGraphql(request: GraphqlOperation): GraphqlResponse =
    withContext(Dispatchers.IO) {
        JsonHelper.jsonDecode(RustNativeBindings.graphql(JsonHelper.jsonEncode(request)))
    }


internal suspend fun executePeerGraphql(peer: Peer, request: GraphqlOperation): GraphqlResponse =
    withContext(Dispatchers.IO) {
        val target = JsonHelper.jsonEncodeToElement(PeerSubscriptionTarget(peer.id, peer.publicKey))
        val body = JsonHelper.jsonEncodeToElement(request).jsonObject + (
            "extensions" to JsonObject(mapOf("nativeTarget" to target))
        )
        val response = JsonHelper.jsonDecode<GraphqlResponse>(RustNativeBindings.graphql(JsonHelper.jsonEncode(JsonObject(body))))
        val error = response.errors.singleOrNull()
        if (response.data == null && error?.extensions == null && error != null) {
            throw BusinessException(error.message)
        }
        response
    }
