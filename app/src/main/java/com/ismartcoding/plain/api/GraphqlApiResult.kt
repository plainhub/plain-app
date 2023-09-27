package com.ismartcoding.plain.api

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.normalized.isFromCache
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import java.lang.Exception

data class GraphqlApiResult<D : Operation.Data>(val response: ApolloResponse<D>?, val exception: Exception? = null) {
    fun isSuccess(): Boolean {
        return response != null && !response.hasErrors()
    }

    fun isRealSuccess(): Boolean {
        return isSuccess() && response?.isFromCache == false
    }

    fun getErrorMessage(): String {
        if (exception != null) {
            return if (exception is BoxUnreachableException) getString(R.string.box_unreachable) else exception.toString()
        } else if (response?.hasErrors() == true) {
            return response.errors?.joinToString(", ") { it.message } ?: ""
        }

        return ""
    }
}
