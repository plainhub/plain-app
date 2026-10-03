package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.mediaactions.NativeMediaActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

actual suspend fun handleMediaActionHost(params: JsonObject): JsonObject = withContext(Dispatchers.IO) { NativeMediaActions.run(params) }
