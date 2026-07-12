package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.db.DFeedEntry

actual suspend fun DFeedEntry.fetchContentAsync(): ApiResult = ApiResult(null, NotImplementedError("fetchContentAsync not implemented on iOS"))
