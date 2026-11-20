package com.ismartcoding.plain.ui.base

import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize
import com.ismartcoding.plain.ui.components.ButtonSize

@Composable
fun PAlertTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val size = ButtonSize.SMALL
    val configuration = LocalViewConfiguration.current
    val alertConfiguration = remember(configuration) {
        object : ViewConfiguration by configuration {
            override val minimumTouchTargetSize: DpSize
                get() = DpSize(configuration.minimumTouchTargetSize.width, size.height)
        }
    }
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides size.height,
        LocalViewConfiguration provides alertConfiguration,
    ) {
        PTextButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            buttonSize = size,
            isLoading = isLoading,
            contentColor = contentColor,
        )
    }
}
