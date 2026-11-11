package com.ismartcoding.plain.ui.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.platform.permissions.requestSystemAccess

internal class AccessSettingsState(
    private val request: suspend (Set<AccessFeature>) -> Boolean = { it.requestSystemAccess() },
    private val update: suspend (Set<AccessFeature>, Boolean) -> Unit = { features, enabled -> Prefs.setApiAccess(features, enabled) },
) {
    var busy by mutableStateOf(false)
        private set
    var deniedFeatures by mutableStateOf(emptySet<AccessFeature>())
        private set

    suspend fun setEnabled(features: Set<AccessFeature>, enabled: Boolean) {
        if (busy) return
        busy = true
        try {
            if (enabled && !request(features)) {
                deniedFeatures = deniedFeatures + features
                return
            }
            update(features, enabled)
            deniedFeatures = deniedFeatures - features
        } finally {
            busy = false
        }
    }
}
