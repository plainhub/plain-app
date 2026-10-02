package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.db.DShare

data class LegacySharedAuth(val share: DShare, val token: ByteArray)
