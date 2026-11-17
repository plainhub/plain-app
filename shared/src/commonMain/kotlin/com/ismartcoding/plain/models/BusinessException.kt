package com.ismartcoding.plain.models


open class BusinessException(val code: String, cause: Throwable? = null, message: String = code) : IllegalStateException(message, cause)
