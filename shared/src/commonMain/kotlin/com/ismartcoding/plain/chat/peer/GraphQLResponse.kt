package com.ismartcoding.plain.chat.peer

data class GraphQLResponse(
    val data: String? = null,
    val errors: List<GraphQLError>? = null,
    val exception: Throwable? = null,
) {
    val isSuccess: Boolean = errors.isNullOrEmpty() && exception == null

    fun getError(): String {
        if (errors?.isNotEmpty() == true) {
            return errors.joinToString(", ") { it.message }
        }

        return exception?.message ?: "Unknown error"
    }
}

data class GraphQLError(
    val message: String,
)
