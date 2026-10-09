package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.loading as ui_string_loading
import org.jetbrains.compose.resources.stringResource

@Composable
fun PFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    type: ButtonType = ButtonType.PRIMARY,
    buttonSize: ButtonSize = ButtonSize.MEDIUM,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val loadingDescription = stringResource(UiRes.string.ui_string_loading)
    val containerColor = type.containerColor()
    val contentColor = type.contentColor()
    val disabledContentColor =
        if (isLoading && enabled) contentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = buttonSize.height)
            .semantics { if (isLoading) stateDescription = loadingDescription },
        shape = CircleShape,
        elevation = buttonSize.elevation(),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = if (isLoading && enabled) containerColor.copy(alpha = 0.8f) else containerColor.copy(alpha = 0.12f),
            disabledContentColor = disabledContentColor,
        ),
        contentPadding = buttonSize.getPaddingValues(),
        enabled = enabled && !isLoading,
    ) {
        ButtonContent(text, icon, buttonSize, isLoading)
    }
}
