package com.ismartcoding.plain.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val IODispatcher: CoroutineDispatcher = Dispatchers.Default
