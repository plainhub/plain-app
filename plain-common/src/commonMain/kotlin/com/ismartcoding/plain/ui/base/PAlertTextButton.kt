package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.DpSize
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.loading as ui_string_loading

@Composable
fun PAlertTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val size = ButtonSize.SMALL
    val interactionSource = remember { MutableInteractionSource() }
    val colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
    val loadingDescription = stringResource(UiRes.string.ui_string_loading)
    val configuration = LocalViewConfiguration.current
    val alertConfiguration = remember(configuration) {
        object : ViewConfiguration by configuration {
            override val minimumTouchTargetSize: DpSize
                get() = DpSize(configuration.minimumTouchTargetSize.width, size.height)
        }
    }
    CompositionLocalProvider(
        LocalContentColor provides if (isLoading) colors.disabledContentColor else colors.contentColor,
        LocalViewConfiguration provides alertConfiguration,
    ) {
        Box(
            modifier = modifier
                .sizeIn(minWidth = ButtonDefaults.MinWidth, minHeight = size.height)
                .semantics { if (isLoading) stateDescription = loadingDescription }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !isLoading,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(Modifier.matchParentSize().clip(CircleShape).indication(interactionSource, ripple()))
            Box(Modifier.padding(horizontal = Spacing.sm)) {
                ButtonContent(text = text, icon = null, size = size, loading = isLoading)
            }
        }
    }
}
