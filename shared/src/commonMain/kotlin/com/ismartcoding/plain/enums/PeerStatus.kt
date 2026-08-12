package com.ismartcoding.plain.enums

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.paired
import com.ismartcoding.plain.i18n.unpaired
import org.jetbrains.compose.resources.stringResource

enum class PeerStatus {
    PAIRED,
    UNPAIRED,
    CHANNEL;

    @Composable
    fun getText(): String {
        return when (this) {
            PAIRED -> stringResource(Res.string.paired)
            UNPAIRED -> stringResource(Res.string.unpaired)
            CHANNEL -> ""
        }
    }
}
