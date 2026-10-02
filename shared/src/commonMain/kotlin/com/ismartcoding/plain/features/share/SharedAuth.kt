package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.db.DShare

data class SharedAuth(val share: DShare, val token: ByteArray)
