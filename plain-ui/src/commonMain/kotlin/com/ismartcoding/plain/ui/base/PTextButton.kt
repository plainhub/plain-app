package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.loading as ui_string_loading
import org.jetbrains.compose.resources.stringResource

@Composable
fun PTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    buttonSize: ButtonSize = ButtonSize.MEDIUM,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val loadingDescription = stringResource(UiRes.string.ui_string_loading)
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = buttonSize.height)
            .semantics { if (isLoading) stateDescription = loadingDescription },
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
        contentPadding = buttonSize.getPaddingValues(),
        enabled = enabled && !isLoading,
    ) {
        ButtonContent(text, icon, buttonSize, isLoading)
    }
}
