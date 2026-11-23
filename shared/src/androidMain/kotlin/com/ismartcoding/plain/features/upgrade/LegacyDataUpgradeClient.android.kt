package com.ismartcoding.plain.features.upgrade

import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.rust.native.nativeCall

internal actual object LegacyDataUpgradeClient {
    actual val supported = true

    actual suspend fun execute(action: LegacyDataUpgradeAction): LegacyDataUpgradeStatus {
        val status = nativeCall<LegacyDataUpgradeStatus> { RustNativeBindings.legacyDataUpgrade(action.name) }
        if (action == LegacyDataUpgradeAction.IMPORT) Prefs.load()
        return status
    }
}
