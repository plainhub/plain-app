package com.ismartcoding.plain.features.upgrade

import kotlinx.serialization.Serializable

@Serializable
internal enum class LegacyDataUpgradeStatus {
    UNAVAILABLE, PENDING, SKIPPED, IMPORTED, CLEARED,
}

internal enum class LegacyDataUpgradeAction {
    STATUS, SKIP, IMPORT, CLEAR,
}

internal expect object LegacyDataUpgradeClient {
    val supported: Boolean
    suspend fun execute(action: LegacyDataUpgradeAction): LegacyDataUpgradeStatus
}
