package com.ismartcoding.plain.platform

import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.appContext

actual fun getDbPath(): String = appContext.getDatabasePath(Constants.DATABASE_NAME).absolutePath
