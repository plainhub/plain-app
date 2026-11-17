package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.models.GraphqlErrorCode
import com.ismartcoding.plain.models.SubscriptionErrorCode

import com.ismartcoding.plain.models.BusinessException
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Serializable
data class GraphqlError(val message: String, val path: JsonArray? = null, val locations: List<GraphqlErrorLocation> = emptyList(), val extensions: JsonObject? = null) {
    val code: String? get() = extensions?.get("code")?.jsonPrimitive?.contentOrNull
}
@Serializable
data class GraphqlErrorLocation(val line: Int, val column: Int)
@Serializable
data class GraphqlResponse(val data: JsonObject? = null, val errors: List<GraphqlError> = emptyList(), val extensions: JsonObject? = null)

open class GraphqlException(val errors: List<GraphqlError>) : BusinessException(
    errors.firstNotNullOfOrNull { it.code } ?: GraphqlErrorCode.INTERNAL_ERROR.wire,
    message = errors.joinToString { it.message },
)

class GraphqlSubscriptionException(errors: List<GraphqlError>) : GraphqlException(errors) {
    val requiresResynchronization: Boolean get() = errors.any { it.code == SubscriptionErrorCode.RESYNC_REQUIRED.wire }
}
