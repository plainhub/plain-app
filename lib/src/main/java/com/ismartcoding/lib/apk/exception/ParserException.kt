package com.ismartcoding.lib.apk.exception

import android.os.Build
import androidx.annotation.RequiresApi

class ParserException : RuntimeException {
    constructor(msg: String?) : super(msg)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)

    @RequiresApi(api = Build.VERSION_CODES.N)
    constructor(
        message: String?, cause: Throwable?, enableSuppression: Boolean,
        writableStackTrace: Boolean
    ) : super(message, cause, enableSuppression, writableStackTrace)

    constructor()
}