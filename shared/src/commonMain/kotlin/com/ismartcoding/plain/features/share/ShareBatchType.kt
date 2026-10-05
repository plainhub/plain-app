package com.ismartcoding.plain.features.share

import kotlinx.serialization.Serializable

@Serializable
enum class ShareBatchType { FILE, ZIP, SYNC, MULTI }
