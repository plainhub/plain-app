package com.ismartcoding.lib.kgraphql.request

import com.ismartcoding.lib.kgraphql.schema.model.ast.DocumentNode
import com.github.benmanes.caffeine.cache.Caffeine


class CachingDocumentParser(cacheMaximumSize : Long = 1000L) {

    sealed class Result {
        class Success(val document: DocumentNode) : Result()
        class Exception(val exception: kotlin.Exception) : Result()
    }

    val cache = Caffeine.newBuilder().maximumSize(cacheMaximumSize).build<String, Result>()

    fun parseDocument(input: String): DocumentNode {
        val result =  cache.get(input) {
            val parser = Parser(input)
            try {
                Result.Success(parser.parseDocument())
            } catch ( e : Exception){
                Result.Exception(e)
            }
        }

        when (result) {
            is Result.Success -> return result.document
            is Result.Exception -> throw result.exception
            else -> {
                cache.invalidateAll()
                throw IllegalStateException("Internal error of CachingDocumentParser")
            }
        }
    }
}
