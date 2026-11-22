package com.ismartcoding.plain.features.upgrade

internal actual object LegacyDataUpgradeClient {
    actual val supported = false
    actual suspend fun execute(action: LegacyDataUpgradeAction) = LegacyDataUpgradeStatus.UNAVAILABLE
}
