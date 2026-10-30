package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.native.RustNativeBindings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

object GraphqlApi : GraphqlClient("native", ::executeNativeGraphql) {
    fun forPeer(peer: com.ismartcoding.plain.models.Peer): GraphqlClient = PeerHttpClient(peer).graphql
}

private suspend fun executeNativeGraphql(request: GraphqlOperation): GraphqlResponse =
    withContext(Dispatchers.IO) {
        JsonHelper.jsonDecode(RustNativeBindings.graphql(JsonHelper.jsonEncode(request)))
    }
