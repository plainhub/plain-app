package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.PAlert

@Composable
fun NetworkErrorBanner(
    isVisible: Boolean,
) {
    if (isVisible) {
        PAlert(
            title = stringResource(R.string.warning),
            description = stringResource(R.string.no_network_connection),
            type = AlertType.WARNING,
        )
    }
} 