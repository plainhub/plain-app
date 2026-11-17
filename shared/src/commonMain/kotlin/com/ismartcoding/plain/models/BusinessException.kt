package com.ismartcoding.plain.models

open class BusinessException(val code: ErrorCode, cause: Throwable? = null, message: String = code.wire) : IllegalStateException(message, cause)
