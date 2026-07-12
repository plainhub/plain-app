package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.db.DFeedEntry

expect suspend fun DFeedEntry.fetchContentAsync(): ApiResult
