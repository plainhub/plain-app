package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.loading as ui_string_loading
import org.jetbrains.compose.resources.stringResource

@Composable
fun POutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    type: ButtonType = ButtonType.PRIMARY,
    buttonSize: ButtonSize = ButtonSize.MEDIUM,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    contentColor: Color? = null,
) {
    val loadingDescription = stringResource(UiRes.string.ui_string_loading)
    val resolvedColor = contentColor ?: type.containerColor()
    val borderColor = resolvedColor.copy(alpha = 0.5f)
    // Disabled follows the Material default graying so icon, text and border
    // mute together instead of a full-color icon beside washed-out text.
    val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = buttonSize.height)
            .semantics { if (isLoading) stateDescription = loadingDescription },
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = resolvedColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContentColor,
        ),
        border = BorderStroke(1.dp, if (enabled && !isLoading) borderColor else disabledBorderColor),
        contentPadding = buttonSize.getPaddingValues(),
        enabled = enabled && !isLoading,
    ) {
        ButtonContent(text, icon, buttonSize, isLoading)
    }
}
