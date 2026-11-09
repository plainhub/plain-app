package com.ismartcoding.plain.tests

import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.testing.CryptoFixtures
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64

internal object ResourceCrypto {
    private fun coreUrlToken(): String = runBlocking {
        GraphqlApi.execute(GraphqlRequest<JsonObject>("query { app { urlToken } }"))
            .getValue("app").jsonObject.getValue("urlToken").jsonPrimitive.content
    }

    fun encrypt(text: String, key: ByteArray = Base64.decode(coreUrlToken())): String =
        Base64.encode(CryptoFixtures.chaCha20Encrypt(key, text))
}
